package com.crob.agent.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName("system_tenant")
@Data
@Accessors(chain = true)
public class SysTenantDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String contactName;
    private String contactMobile;
    private Integer status;
    private String domain;
    private String packageId;
    private LocalDateTime expireTime;
    private Integer accountCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
