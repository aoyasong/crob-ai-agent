package com.crob.agent.module.crob.service.task;

import static com.crob.agent.framework.common.pojo.CommonResult.success;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crob.agent.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.crob.agent.framework.common.exception.util.ServiceExceptionUtil;
import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.framework.common.util.object.BeanUtils;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobAttemptRespVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskCreateReqVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskInputsReqVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskRerunReqVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskRespVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskStreamRespVO;
import com.crob.agent.module.crob.dal.dataobject.attempt.CrobAttemptDO;
import com.crob.agent.module.crob.dal.dataobject.task.CrobTaskDO;
import com.crob.agent.module.crob.dal.mysql.attempt.CrobAttemptMapper;
import com.crob.agent.module.crob.dal.mysql.task.CrobTaskMapper;
import com.crob.agent.module.crob.service.task.executor.CrobExecutorClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class CrobTaskServiceImpl implements CrobTaskService {

    private static final String STATUS_WAITING_INPUT = "WAITING_INPUT";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String DISP_ALLOW = "ALLOW";
    private static final String DISP_ASK_CLARIFY = "ASK_CLARIFY";
    private static final String DISP_REJECT = "REJECT";

    @Resource private CrobTaskMapper taskMapper;
    @Resource private CrobAttemptMapper attemptMapper;
    @Resource private StringRedisTemplate stringRedisTemplate;
    @Resource private CrobExecutorClient executorClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrobTaskRespVO createTask(CrobTaskCreateReqVO reqVO, String idempotencyKey) {
        if (StrUtil.isNotBlank(idempotencyKey)) {
            String existed =
                    stringRedisTemplate.opsForValue().get("crob:idemp:create:" + idempotencyKey);
            if (StrUtil.isNotBlank(existed)) {
                return getTask(Long.valueOf(existed));
            }
        }

        Map<String, Object> inputs =
                reqVO.getInputs() != null ? new HashMap<>(reqVO.getInputs()) : new HashMap<>();
        ValidationResult vr = validate(reqVO.getScenario(), inputs);

        LocalDateTime now = LocalDateTime.now();
        CrobTaskDO task =
                new CrobTaskDO()
                        .setScenario(reqVO.getScenario())
                        .setInputsJson(inputs)
                        .setCreatedAt(now)
                        .setUpdatedAt(now)
                        .setCreatedBy("SYSTEM")
                        .setUpdatedBy("SYSTEM");

        if (!vr.missingFields.isEmpty()) {
            task.setStatus(STATUS_WAITING_INPUT);
            taskMapper.insert(task);
            if (StrUtil.isNotBlank(idempotencyKey)) {
                stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent(
                                "crob:idemp:create:" + idempotencyKey,
                                String.valueOf(task.getTaskId()),
                                Duration.ofDays(1));
            }
            return buildResp(task, DISP_ASK_CLARIFY, vr.missingFields);
        }
        if (vr.rejected) {
            task.setStatus(STATUS_REJECTED)
                    .setErrorCode("E_INPUT_INVALID")
                    .setErrorMessage(vr.rejectReason);
            taskMapper.insert(task);
            if (StrUtil.isNotBlank(idempotencyKey)) {
                stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent(
                                "crob:idemp:create:" + idempotencyKey,
                                String.valueOf(task.getTaskId()),
                                Duration.ofDays(1));
            }
            return buildResp(task, DISP_REJECT, Collections.emptyList());
        }

        task.setStatus(STATUS_QUEUED).setErrorCode(null).setErrorMessage(null);
        taskMapper.insert(task);

        CrobAttemptDO attempt =
                new CrobAttemptDO()
                        .setTaskId(task.getTaskId())
                        .setScenario(task.getScenario())
                        .setStatus(STATUS_QUEUED)
                        .setCreatedAt(now)
                        .setUpdatedAt(now)
                        .setCreatedBy("SYSTEM")
                        .setUpdatedBy("SYSTEM");
        attemptMapper.insert(attempt);

        task.setCurrentAttemptId(attempt.getAttemptId()).setUpdatedAt(now).setUpdatedBy("SYSTEM");
        taskMapper.updateById(task);

        if (StrUtil.isNotBlank(idempotencyKey)) {
            stringRedisTemplate
                    .opsForValue()
                    .setIfAbsent(
                            "crob:idemp:create:" + idempotencyKey,
                            String.valueOf(task.getTaskId()),
                            Duration.ofDays(1));
        }

        // 事务提交后派发给执行面，避免 HTTP 调用异常影响任务持久化
        final Long createTaskId = task.getTaskId();
        final Long createAttemptId = attempt.getAttemptId();
        final String createScenario = task.getScenario();
        final Map<String, Object> createInputs = new HashMap<>(inputs);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        dispatchToExecutor(
                                createTaskId, createAttemptId, createScenario, createInputs);
                    }
                });
        return buildResp(task, DISP_ALLOW, Collections.emptyList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrobTaskRespVO submitInputs(Long taskId, CrobTaskInputsReqVO reqVO) {
        CrobTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND);
        }
        Map<String, Object> merged =
                task.getInputsJson() != null
                        ? new HashMap<>(task.getInputsJson())
                        : new HashMap<>();
        if (reqVO.getInputs() != null) {
            merged.putAll(reqVO.getInputs());
        }
        ValidationResult vr = validate(task.getScenario(), merged);

        LocalDateTime now = LocalDateTime.now();
        task.setInputsJson(merged).setUpdatedAt(now).setUpdatedBy("SYSTEM");

        if (!vr.missingFields.isEmpty()) {
            task.setStatus(STATUS_WAITING_INPUT);
            taskMapper.updateById(task);
            return buildResp(task, DISP_ASK_CLARIFY, vr.missingFields);
        }
        if (vr.rejected) {
            task.setStatus(STATUS_REJECTED)
                    .setErrorCode("E_INPUT_INVALID")
                    .setErrorMessage(vr.rejectReason);
            taskMapper.updateById(task);
            return buildResp(task, DISP_REJECT, Collections.emptyList());
        }

        if (!STATUS_WAITING_INPUT.equals(task.getStatus())) {
            return buildResp(task, DISP_ALLOW, Collections.emptyList());
        }

        task.setStatus(STATUS_QUEUED).setErrorCode(null).setErrorMessage(null);
        taskMapper.updateById(task);

        CrobAttemptDO attempt =
                new CrobAttemptDO()
                        .setTaskId(task.getTaskId())
                        .setScenario(task.getScenario())
                        .setStatus(STATUS_QUEUED)
                        .setCreatedAt(now)
                        .setUpdatedAt(now)
                        .setCreatedBy("SYSTEM")
                        .setUpdatedBy("SYSTEM");
        attemptMapper.insert(attempt);

        task.setCurrentAttemptId(attempt.getAttemptId()).setUpdatedAt(now).setUpdatedBy("SYSTEM");
        taskMapper.updateById(task);

        final Long submitTaskId = task.getTaskId();
        final Long submitAttemptId = attempt.getAttemptId();
        final String submitScenario = task.getScenario();
        final Map<String, Object> submitInputsCopy = new HashMap<>(merged);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        dispatchToExecutor(
                                submitTaskId, submitAttemptId, submitScenario, submitInputsCopy);
                    }
                });
        return buildResp(task, DISP_ALLOW, Collections.emptyList());
    }

    @Override
    public CrobTaskRespVO getTask(Long taskId) {
        CrobTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND);
        }
        String disposition = DISP_ALLOW;
        List<String> missingFields = Collections.emptyList();
        if (STATUS_WAITING_INPUT.equals(task.getStatus())) {
            Map<String, Object> inputs =
                    task.getInputsJson() != null
                            ? new HashMap<>(task.getInputsJson())
                            : new HashMap<>();
            ValidationResult vr = validate(task.getScenario(), inputs);
            disposition = DISP_ASK_CLARIFY;
            missingFields = vr.missingFields;
        } else if (STATUS_REJECTED.equals(task.getStatus())) {
            disposition = DISP_REJECT;
        }
        CrobTaskRespVO resp = BeanUtils.toBean(task, CrobTaskRespVO.class);
        resp.setDisposition(disposition);
        resp.setMissingFields(missingFields);
        if (task.getCurrentAttemptId() != null) {
            resp.setReportUrl(
                    "/tasks/" + taskId + "/report?attempt_id=" + task.getCurrentAttemptId());
        }
        return resp;
    }

    @Override
    public List<CrobAttemptRespVO> listAttempts(Long taskId) {
        CrobTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND);
        }
        List<CrobAttemptDO> list =
                attemptMapper.selectList(
                        new LambdaQueryWrapper<CrobAttemptDO>()
                                .eq(CrobAttemptDO::getTaskId, taskId)
                                .orderByDesc(CrobAttemptDO::getAttemptId));
        return BeanUtils.toBean(list, CrobAttemptRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrobTaskRespVO cancel(Long taskId, Long attemptId) {
        CrobTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND);
        }
        Long effectiveAttemptId = attemptId != null ? attemptId : task.getCurrentAttemptId();
        if (effectiveAttemptId == null) {
            return getTask(taskId);
        }
        CrobAttemptDO attempt = attemptMapper.selectById(effectiveAttemptId);
        if (attempt == null || !Objects.equals(attempt.getTaskId(), taskId)) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND);
        }
        if (STATUS_SUCCEEDED.equals(attempt.getStatus())
                || STATUS_FAILED.equals(attempt.getStatus())
                || STATUS_CANCELLED.equals(attempt.getStatus())) {
            return getTask(taskId);
        }

        stringRedisTemplate
                .opsForValue()
                .set("crob:cancel:" + effectiveAttemptId, "1", Duration.ofDays(1));

        LocalDateTime now = LocalDateTime.now();
        attempt.setStatus(STATUS_CANCELLED)
                .setEndedAt(now)
                .setUpdatedAt(now)
                .setUpdatedBy("SYSTEM");
        attemptMapper.updateById(attempt);
        task.setStatus(STATUS_CANCELLED).setUpdatedAt(now).setUpdatedBy("SYSTEM");
        taskMapper.updateById(task);
        return getTask(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrobTaskRespVO rerun(Long taskId, CrobTaskRerunReqVO reqVO, String idempotencyKey) {
        CrobTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw ServiceExceptionUtil.exception(GlobalErrorCodeConstants.NOT_FOUND);
        }
        if (StrUtil.isNotBlank(idempotencyKey)) {
            String existed =
                    stringRedisTemplate
                            .opsForValue()
                            .get("crob:idemp:rerun:" + taskId + ":" + idempotencyKey);
            if (StrUtil.isNotBlank(existed)) {
                Long existedAttemptId = Long.valueOf(existed);
                if (!Objects.equals(task.getCurrentAttemptId(), existedAttemptId)) {
                    task.setCurrentAttemptId(existedAttemptId)
                            .setUpdatedAt(LocalDateTime.now())
                            .setUpdatedBy("SYSTEM");
                    taskMapper.updateById(task);
                }
                return getTask(taskId);
            }
        }

        Map<String, Object> inputs =
                task.getInputsJson() != null
                        ? new HashMap<>(task.getInputsJson())
                        : new HashMap<>();
        ValidationResult vr = validate(task.getScenario(), inputs);
        if (!vr.missingFields.isEmpty()) {
            task.setStatus(STATUS_WAITING_INPUT)
                    .setUpdatedAt(LocalDateTime.now())
                    .setUpdatedBy("SYSTEM");
            taskMapper.updateById(task);
            return buildResp(task, DISP_ASK_CLARIFY, vr.missingFields);
        }
        if (vr.rejected) {
            task.setStatus(STATUS_REJECTED)
                    .setErrorCode("E_INPUT_INVALID")
                    .setErrorMessage(vr.rejectReason)
                    .setUpdatedAt(LocalDateTime.now())
                    .setUpdatedBy("SYSTEM");
            taskMapper.updateById(task);
            return buildResp(task, DISP_REJECT, Collections.emptyList());
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> override =
                reqVO != null && reqVO.getOverrideConstraints() != null
                        ? new HashMap<>(reqVO.getOverrideConstraints())
                        : Collections.emptyMap();

        CrobAttemptDO attempt =
                new CrobAttemptDO()
                        .setTaskId(task.getTaskId())
                        .setScenario(task.getScenario())
                        .setStatus(STATUS_QUEUED)
                        .setEffectiveConstraintsJson(override)
                        .setCreatedAt(now)
                        .setUpdatedAt(now)
                        .setCreatedBy("SYSTEM")
                        .setUpdatedBy("SYSTEM");
        attemptMapper.insert(attempt);

        task.setStatus(STATUS_QUEUED)
                .setCurrentAttemptId(attempt.getAttemptId())
                .setErrorCode(null)
                .setErrorMessage(null)
                .setUpdatedAt(now)
                .setUpdatedBy("SYSTEM");
        taskMapper.updateById(task);

        if (StrUtil.isNotBlank(idempotencyKey)) {
            stringRedisTemplate
                    .opsForValue()
                    .setIfAbsent(
                            "crob:idemp:rerun:" + taskId + ":" + idempotencyKey,
                            String.valueOf(attempt.getAttemptId()),
                            Duration.ofDays(1));
        }

        final Long rerunTaskId = task.getTaskId();
        final Long rerunAttemptId = attempt.getAttemptId();
        final String rerunScenario = task.getScenario();
        final Map<String, Object> rerunInputs = new HashMap<>(inputs);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        dispatchToExecutor(rerunTaskId, rerunAttemptId, rerunScenario, rerunInputs);
                    }
                });
        return buildResp(task, DISP_ALLOW, Collections.emptyList());
    }

    @Override
    public Flux<CommonResult<CrobTaskStreamRespVO>> stream(Long taskId, Long attemptId) {
        if (attemptId == null) {
            return Flux.just(
                    success(
                            new CrobTaskStreamRespVO()
                                    .setType("L6_ERROR")
                                    .setDelta("attempt_id is required")));
        }
        CrobAttemptDO attempt = attemptMapper.selectById(attemptId);
        if (attempt == null || !Objects.equals(attempt.getTaskId(), taskId)) {
            return Flux.just(
                    success(
                            new CrobTaskStreamRespVO()
                                    .setType("L6_ERROR")
                                    .setDelta("attempt not found")));
        }

        String streamKey = "crob:stream:" + taskId + ":" + attemptId;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return Flux.<CommonResult<CrobTaskStreamRespVO>>create(
                sink -> {
                    sink.onDispose(executor::shutdownNow);
                    executor.submit(
                            () -> {
                                StreamOffset<String> offset =
                                        StreamOffset.create(streamKey, ReadOffset.from("0-0"));
                                StreamReadOptions options =
                                        StreamReadOptions.empty()
                                                .count(20)
                                                .block(Duration.ofSeconds(2));
                                while (!sink.isCancelled()) {
                                    List<MapRecord<String, Object, Object>> records =
                                            stringRedisTemplate
                                                    .opsForStream()
                                                    .read(options, offset);
                                    if (records == null || records.isEmpty()) {
                                        sink.next(
                                                success(
                                                        new CrobTaskStreamRespVO()
                                                                .setType("PING")));
                                        if (isAttemptTerminal(attemptId)) {
                                            sink.complete();
                                            return;
                                        }
                                        continue;
                                    }
                                    for (MapRecord<String, Object, Object> record : records) {
                                        Object deltaObj = record.getValue().get("delta");
                                        String delta =
                                                deltaObj != null ? String.valueOf(deltaObj) : null;
                                        if (delta != null) {
                                            sink.next(
                                                    success(
                                                            new CrobTaskStreamRespVO()
                                                                    .setType("L6_DELTA")
                                                                    .setDelta(delta)));
                                        }
                                        offset =
                                                StreamOffset.create(
                                                        streamKey,
                                                        ReadOffset.from(record.getId().getValue()));
                                    }
                                    if (isAttemptTerminal(attemptId)) {
                                        sink.complete();
                                        return;
                                    }
                                }
                            });
                });
    }

    private boolean isAttemptTerminal(Long attemptId) {
        CrobAttemptDO attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            return true;
        }
        String status = attempt.getStatus();
        return STATUS_SUCCEEDED.equals(status)
                || STATUS_FAILED.equals(status)
                || STATUS_CANCELLED.equals(status);
    }

    private CrobTaskRespVO buildResp(
            CrobTaskDO task, String disposition, List<String> missingFields) {
        CrobTaskRespVO resp = BeanUtils.toBean(task, CrobTaskRespVO.class);
        resp.setDisposition(disposition);
        resp.setMissingFields(missingFields);
        if (task.getCurrentAttemptId() != null) {
            resp.setReportUrl(
                    "/tasks/"
                            + task.getTaskId()
                            + "/report?attempt_id="
                            + task.getCurrentAttemptId());
        }
        return resp;
    }

    private ValidationResult validate(String scenario, Map<String, Object> inputs) {
        ValidationResult vr = new ValidationResult();
        if ("MARKET_ANALYSIS".equalsIgnoreCase(scenario)) {
            require(inputs, vr, "category_or_keyword");
            require(inputs, vr, "platform");
            require(inputs, vr, "market");
            rejectIfEquals(inputs, vr, "platform", "OTHER");
            rejectIfEquals(inputs, vr, "market", "OTHER");
            return vr;
        }
        if ("PROFIT_MODEL".equalsIgnoreCase(scenario)) {
            require(inputs, vr, "platform");
            require(inputs, vr, "market");
            require(inputs, vr, "shipping_mode");
            require(inputs, vr, "cost_fen");
            require(inputs, vr, "price_fen");
            rejectIfEquals(inputs, vr, "shipping_mode", "OTHER");
            rejectIfEquals(inputs, vr, "platform", "OTHER");
            rejectIfEquals(inputs, vr, "market", "OTHER");
            Integer cost = asInt(inputs.get("cost_fen"));
            Integer price = asInt(inputs.get("price_fen"));
            if (cost != null && price != null && price < cost) {
                require(inputs, vr, "confirm_negative_profit");
            }
            return vr;
        }
        if ("CHAT".equalsIgnoreCase(scenario)) {
            require(inputs, vr, "prompt");
            return vr;
        }
        vr.rejected = true;
        vr.rejectReason = "SCENARIO_UNSUPPORTED";
        return vr;
    }

    private void require(Map<String, Object> inputs, ValidationResult vr, String key) {
        Object v = inputs.get(key);
        if (v == null || (v instanceof String && StrUtil.isBlank((String) v))) {
            vr.missingFields.add(key);
        }
    }

    private void rejectIfEquals(
            Map<String, Object> inputs, ValidationResult vr, String key, String value) {
        Object v = inputs.get(key);
        if (v == null) {
            return;
        }
        if (value.equalsIgnoreCase(String.valueOf(v))) {
            vr.rejected = true;
            vr.rejectReason = key + "_NOT_SUPPORTED";
        }
    }

    private Integer asInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Integer) {
            return (Integer) o;
        }
        if (o instanceof Long) {
            return ((Long) o).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class ValidationResult {
        private final List<String> missingFields = new ArrayList<>();
        private boolean rejected;
        private String rejectReason;
    }

    @Scheduled(fixedRate = 30000)
    public void scanTimeoutAttempts() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(5);
        List<CrobAttemptDO> running =
                attemptMapper.selectList(
                        new LambdaQueryWrapper<CrobAttemptDO>()
                                .eq(CrobAttemptDO::getStatus, STATUS_RUNNING)
                                .lt(CrobAttemptDO::getStartedAt, timeout));
        LocalDateTime now = LocalDateTime.now();
        for (CrobAttemptDO a : running) {
            a.setStatus(STATUS_FAILED)
                    .setEndedAt(now)
                    .setErrorCode("E_TIMEOUT")
                    .setErrorMessage("Attempt timed out")
                    .setUpdatedAt(now)
                    .setUpdatedBy("SYSTEM");
            attemptMapper.updateById(a);
            CrobTaskDO t = taskMapper.selectById(a.getTaskId());
            if (t != null) {
                t.setStatus(STATUS_FAILED)
                        .setErrorCode("E_TIMEOUT")
                        .setErrorMessage("Attempt timed out")
                        .setUpdatedAt(now)
                        .setUpdatedBy("SYSTEM");
                taskMapper.updateById(t);
            }
        }
    }

    private void dispatchToExecutor(
            Long taskId, Long attemptId, String scenario, Map<String, Object> inputs) {
        try {
            executorClient.execute(taskId, attemptId, scenario, inputs, Collections.emptyMap());
        } catch (Exception e) {
            log.error("Failed to dispatch task {} to executor: {}", taskId, e.getMessage());
            CrobTaskDO task = taskMapper.selectById(taskId);
            if (task != null) {
                task.setStatus(STATUS_FAILED)
                        .setErrorCode("E_EXECUTOR_FAILED")
                        .setErrorMessage("调度执行面失败: " + e.getMessage())
                        .setUpdatedAt(LocalDateTime.now())
                        .setUpdatedBy("SYSTEM");
                taskMapper.updateById(task);
            }
        }
    }
}
