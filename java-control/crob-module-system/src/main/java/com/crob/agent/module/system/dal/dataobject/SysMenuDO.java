package com.crob.agent.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName("system_menu")
@Data
@Accessors(chain = true)
public class SysMenuDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String permission;
    private Integer type;
    private Integer sort;
    private Long parentId;
    private String path;
    private String icon;
    private String component;
    private String componentName;
    private Integer status;
    private Boolean visible;
    private Boolean keepAlive;
    private Boolean alwaysShow;
    private LocalDateTime createTime;
}
