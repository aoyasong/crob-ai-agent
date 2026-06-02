package com.crob.agent.module.crob.controller.admin.task.vo;

import java.util.Map;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CrobTaskCreateReqVO {

    @NotBlank private String scenario;

    private Map<String, Object> inputs;
}
