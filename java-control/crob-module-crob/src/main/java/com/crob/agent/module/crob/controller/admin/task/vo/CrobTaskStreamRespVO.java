package com.crob.agent.module.crob.controller.admin.task.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CrobTaskStreamRespVO {

    private String type;

    private String delta;
}
