package com.crob.agent.module.crob.dal.dataobject.event;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName(value = "crob_usage_event", autoResultMap = true)
@Data
@Accessors(chain = true)
public class CrobUsageEventDO {

    @TableId(value = "usage_id", type = IdType.AUTO)
    private Long usageId;

    private Long taskId;

    private Long attemptId;

    private Integer seq;

    private String provider;

    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Integer costFen;

    @TableField(value = "raw_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> rawJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
