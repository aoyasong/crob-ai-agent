package com.crob.agent.module.crob.controller.internal.vo;

import lombok.Data;

@Data
public class InternalTaskEventsReqVO {

    private InternalAttemptEventReqVO attemptEvent;

    private InternalUsageEventReqVO usageEvent;
}
