package com.crob.agent.module.system.controller.admin.user.vo;

import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UserCreateReqVO {

    @NotEmpty(message = "用户名不能为空")
    private String username;

    @NotEmpty(message = "密码不能为空")
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
