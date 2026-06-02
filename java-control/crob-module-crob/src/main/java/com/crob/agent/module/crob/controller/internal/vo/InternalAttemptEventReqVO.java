package com.crob.agent.module.crob.controller.internal.vo;

import java.util.Map;
import lombok.Data;

@Data
public class InternalAttemptEventReqVO {

    private Long taskId;

    private Long attemptId;

    private Integer seq;

    private String eventType;

    private Map<String, Object> payload;

    private String errorCode;

    private String errorMessage;
}
