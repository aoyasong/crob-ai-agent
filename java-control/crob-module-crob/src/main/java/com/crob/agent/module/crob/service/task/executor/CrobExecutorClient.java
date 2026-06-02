package com.crob.agent.module.crob.service.task.executor;

import java.util.Map;

public interface CrobExecutorClient {

    void execute(
            Long taskId,
            Long attemptId,
            String scenario,
            Map<String, Object> inputs,
            Map<String, Object> effectiveConstraints);
}
