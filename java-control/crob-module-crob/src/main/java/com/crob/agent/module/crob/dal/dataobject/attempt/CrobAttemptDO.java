package com.crob.agent.module.crob.dal.dataobject.attempt;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName(value = "crob_attempt", autoResultMap = true)
@Data
@Accessors(chain = true)
public class CrobAttemptDO {

    @TableId(value = "attempt_id", type = IdType.ASSIGN_ID)
    private Long attemptId;

    private Long taskId;

    private String scenario;

    private String status;

    private String priceVersionSnapshot;

    @TableField(value = "effective_constraints_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> effectiveConstraintsJson;

    private String policyVersion;

    private LocalDateTime lastHeartbeatAt;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private String errorCode;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
