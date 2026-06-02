package com.crob.agent.module.crob.controller.admin.task.vo;

import java.util.List;
import lombok.Data;

@Data
public class CrobTaskRespVO {

    private Long taskId;

    private String scenario;

    private String status;

    private Long currentAttemptId;

    private Long latestSuccessAttemptId;

    private String disposition;

    private List<String> missingFields;

    private String reportUrl;

    private String errorCode;

    private String errorMessage;
}
