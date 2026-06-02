package com.crob.agent.module.crob.controller.internal;

import static com.crob.agent.framework.common.pojo.CommonResult.success;

import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.module.crob.controller.internal.vo.InternalAttemptEventReqVO;
import com.crob.agent.module.crob.controller.internal.vo.InternalTaskEventsReqVO;
import com.crob.agent.module.crob.controller.internal.vo.InternalUsageEventReqVO;
import com.crob.agent.module.crob.dal.dataobject.attempt.CrobAttemptDO;
import com.crob.agent.module.crob.dal.dataobject.event.CrobTaskEventDO;
import com.crob.agent.module.crob.dal.dataobject.event.CrobUsageEventDO;
import com.crob.agent.module.crob.dal.dataobject.task.CrobTaskDO;
import com.crob.agent.module.crob.dal.mysql.attempt.CrobAttemptMapper;
import com.crob.agent.module.crob.dal.mysql.event.CrobTaskEventMapper;
import com.crob.agent.module.crob.dal.mysql.event.CrobUsageEventMapper;
import com.crob.agent.module.crob.dal.mysql.task.CrobTaskMapper;
import java.time.LocalDateTime;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CrobInternalTaskEventsController {

    @Resource private CrobTaskMapper taskMapper;
    @Resource private CrobAttemptMapper attemptMapper;
    @Resource private CrobTaskEventMapper taskEventMapper;
    @Resource private CrobUsageEventMapper usageEventMapper;

    @PostMapping("/internal/task-events")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Boolean> receive(@Valid @RequestBody InternalTaskEventsReqVO reqVO) {
        LocalDateTime now = LocalDateTime.now();
        if (reqVO.getAttemptEvent() != null) {
            handleAttemptEvent(reqVO.getAttemptEvent(), now);
        }
        if (reqVO.getUsageEvent() != null) {
            handleUsageEvent(reqVO.getUsageEvent(), now);
        }
        return success(true);
    }

    private void handleAttemptEvent(InternalAttemptEventReqVO event, LocalDateTime now) {
        CrobTaskEventDO doObj =
                new CrobTaskEventDO()
                        .setTaskId(event.getTaskId())
                        .setAttemptId(event.getAttemptId())
                        .setSeq(event.getSeq())
                        .setEventType(event.getEventType())
                        .setPayloadJson(event.getPayload())
                        .setCreatedAt(now)
                        .setUpdatedAt(now)
                        .setCreatedBy("EXECUTOR:python")
                        .setUpdatedBy("EXECUTOR:python");
        try {
            taskEventMapper.insert(doObj);
        } catch (DuplicateKeyException ignore) {
        }

        CrobAttemptDO attempt = attemptMapper.selectById(event.getAttemptId());
        if (attempt == null) {
            return;
        }
        CrobTaskDO task = taskMapper.selectById(event.getTaskId());
        if (task == null) {
            return;
        }

        if ("ATTEMPT_STARTED".equals(event.getEventType())) {
            attempt.setStatus("RUNNING")
                    .setStartedAt(now)
                    .setUpdatedAt(now)
                    .setUpdatedBy("EXECUTOR:python");
            attemptMapper.updateById(attempt);
            task.setStatus("RUNNING").setUpdatedAt(now).setUpdatedBy("EXECUTOR:python");
            taskMapper.updateById(task);
            return;
        }
        if ("HEARTBEAT".equals(event.getEventType())) {
            attempt.setLastHeartbeatAt(now).setUpdatedAt(now).setUpdatedBy("EXECUTOR:python");
            attemptMapper.updateById(attempt);
            return;
        }
        if ("ATTEMPT_SUCCEEDED".equals(event.getEventType())) {
            attempt.setStatus("SUCCEEDED")
                    .setEndedAt(now)
                    .setUpdatedAt(now)
                    .setUpdatedBy("EXECUTOR:python");
            attemptMapper.updateById(attempt);
            task.setStatus("SUCCEEDED")
                    .setLatestSuccessAttemptId(attempt.getAttemptId())
                    .setUpdatedAt(now)
                    .setUpdatedBy("EXECUTOR:python");
            taskMapper.updateById(task);
            return;
        }
        if ("ATTEMPT_FAILED".equals(event.getEventType())) {
            attempt.setStatus("FAILED")
                    .setEndedAt(now)
                    .setErrorCode(event.getErrorCode())
                    .setErrorMessage(event.getErrorMessage())
                    .setUpdatedAt(now)
                    .setUpdatedBy("EXECUTOR:python");
            attemptMapper.updateById(attempt);
            task.setStatus("FAILED")
                    .setErrorCode(event.getErrorCode())
                    .setErrorMessage(event.getErrorMessage())
                    .setUpdatedAt(now)
                    .setUpdatedBy("EXECUTOR:python");
            taskMapper.updateById(task);
            return;
        }
        if ("ATTEMPT_CANCELLED".equals(event.getEventType())) {
            attempt.setStatus("CANCELLED")
                    .setEndedAt(now)
                    .setUpdatedAt(now)
                    .setUpdatedBy("EXECUTOR:python");
            attemptMapper.updateById(attempt);
            task.setStatus("CANCELLED").setUpdatedAt(now).setUpdatedBy("EXECUTOR:python");
            taskMapper.updateById(task);
        }
    }

    private void handleUsageEvent(InternalUsageEventReqVO event, LocalDateTime now) {
        CrobUsageEventDO doObj =
                new CrobUsageEventDO()
                        .setTaskId(event.getTaskId())
                        .setAttemptId(event.getAttemptId())
                        .setSeq(event.getSeq())
                        .setProvider(event.getProvider())
                        .setModel(event.getModel())
                        .setPromptTokens(event.getPromptTokens())
                        .setCompletionTokens(event.getCompletionTokens())
                        .setTotalTokens(event.getTotalTokens())
                        .setRawJson(event.getRaw())
                        .setCreatedAt(now)
                        .setUpdatedAt(now)
                        .setCreatedBy("EXECUTOR:python")
                        .setUpdatedBy("EXECUTOR:python");
        try {
            usageEventMapper.insert(doObj);
        } catch (DuplicateKeyException ignore) {
        }
    }
}
