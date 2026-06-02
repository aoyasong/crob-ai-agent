package com.crob.agent.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName("system_users")
@Data
@Accessors(chain = true)
public class SysUserDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private Long deptId;
    private String email;
    private String mobile;
    private Integer sex;
    private String avatar;
    private Integer status;
    private String loginIp;
    private LocalDateTime loginDate;
    private String remark;
    private Long tenantId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String creator;
    private String updater;
}
