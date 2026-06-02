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

@TableName(value = "crob_task_event", autoResultMap = true)
@Data
@Accessors(chain = true)
public class CrobTaskEventDO {

    @TableId(value = "event_id", type = IdType.AUTO)
    private Long eventId;

    private Long taskId;

    private Long attemptId;

    private Integer seq;

    private String eventType;

    @TableField(value = "payload_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> payloadJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
