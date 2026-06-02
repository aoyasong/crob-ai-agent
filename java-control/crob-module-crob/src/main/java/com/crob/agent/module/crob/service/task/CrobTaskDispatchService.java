package com.crob.agent.module.crob.service.task;

import com.crob.agent.module.crob.dal.dataobject.task.CrobTaskDO;
import com.crob.agent.module.crob.dal.mysql.task.CrobTaskMapper;
import com.crob.agent.module.crob.service.task.executor.CrobExecutorClient;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 任务调度服务 — 在独立事务中将任务派发给 Python 执行面。 与创建任务的 DB 事务分离，避免 executor HTTP 调用异常影响任务持久化。 */
@Slf4j
@Service
public class CrobTaskDispatchService {

    @Resource private CrobExecutorClient executorClient;

    @Resource private CrobTaskMapper taskMapper;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(Long taskId, Long attemptId, String scenario, Map<String, Object> inputs) {
        try {
            executorClient.execute(taskId, attemptId, scenario, inputs, Collections.emptyMap());
        } catch (Exception e) {
            log.error("Failed to dispatch task {} to executor: {}", taskId, e.getMessage());
            CrobTaskDO task = taskMapper.selectById(taskId);
            if (task != null) {
                task.setStatus("FAILED")
                        .setErrorCode("E_EXECUTOR_FAILED")
                        .setErrorMessage("调度执行面失败: " + e.getMessage())
                        .setUpdatedAt(LocalDateTime.now())
                        .setUpdatedBy("SYSTEM");
                taskMapper.updateById(task);
            }
        }
    }
}
