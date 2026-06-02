package com.crob.agent.module.crob.dal.dataobject.result;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName("crob_task_result")
@Data
@Accessors(chain = true)
public class CrobTaskResultDO {

    private Long taskId;

    private Long attemptId;

    private String reportMd;

    private String resultJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
