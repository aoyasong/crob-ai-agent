package com.crob.agent.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName("system_role")
@Data
@Accessors(chain = true)
public class SysRoleDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private String remark;
    private Long tenantId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
