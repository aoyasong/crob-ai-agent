package com.crob.agent.module.crob.controller.admin.task.vo;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

@Data
public class CrobAttemptRespVO {

    private Long attemptId;

    private Long taskId;

    private String scenario;

    private String status;

    private Map<String, Object> effectiveConstraintsJson;

    private LocalDateTime lastHeartbeatAt;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private String errorCode;

    private String errorMessage;

    private LocalDateTime createdAt;
}
