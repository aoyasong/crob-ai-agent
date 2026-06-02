package com.crob.agent.module.crob.dal.dataobject.artifact;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName("crob_artifact")
@Data
@Accessors(chain = true)
public class CrobArtifactDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long artifactId;

    private Long taskId;
    private Long attemptId;
    private String bucket;
    private String objectKey;
    private String filename;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
