package com.crob.agent.module.crob.dal.dataobject.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName(value = "crob_task", autoResultMap = true)
@Data
@Accessors(chain = true)
public class CrobTaskDO {

    @TableId(value = "task_id", type = IdType.ASSIGN_ID)
    private Long taskId;

    private String scenario;

    private String status;

    @TableField(value = "inputs_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputsJson;

    private Long currentAttemptId;

    private Long latestSuccessAttemptId;

    private String errorCode;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
