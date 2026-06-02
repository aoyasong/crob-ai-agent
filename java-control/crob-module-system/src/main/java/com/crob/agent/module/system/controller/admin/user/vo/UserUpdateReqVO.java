package com.crob.agent.module.system.controller.admin.user.vo;

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateReqVO {

    @NotNull(message = "用户ID不能为空")
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
    private String remark;
}
