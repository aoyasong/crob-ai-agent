package com.crob.agent.module.crob.controller.admin.task.vo;

import java.util.Map;
import lombok.Data;

@Data
public class CrobTaskRerunReqVO {

    private Map<String, Object> overrideConstraints;
}
