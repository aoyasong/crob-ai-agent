package com.crob.agent.module.system.controller.admin.user.vo;

import lombok.Data;

@Data
public class UserPageReqVO {

    private Integer pageNo = 1;
    private Integer pageSize = 10;
    private String username;
    private String nickname;
    private Integer status;
}
