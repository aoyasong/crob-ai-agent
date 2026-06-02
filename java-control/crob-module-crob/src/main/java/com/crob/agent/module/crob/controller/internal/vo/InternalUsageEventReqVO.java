package com.crob.agent.module.crob.controller.internal.vo;

import java.util.Map;
import lombok.Data;

@Data
public class InternalUsageEventReqVO {

    private Long taskId;

    private Long attemptId;

    private Integer seq;

    private String provider;

    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Map<String, Object> raw;
}
