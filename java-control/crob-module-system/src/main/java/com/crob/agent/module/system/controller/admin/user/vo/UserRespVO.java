package com.crob.agent.module.system.controller.admin.user.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserRespVO {

    private Long id;
    private String username;
    private String nickname;
    private Long deptId;
    private String email;
    private String mobile;
    private Integer sex;
    private String avatar;
    private Integer status;
    private String remark;
    private String loginIp;
    private LocalDateTime loginDate;
    private LocalDateTime createTime;

    public String[] getPostIds() {
        return new String[0];
    }
}
