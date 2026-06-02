package com.crob.agent.module.system.controller.admin.auth.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AuthLoginRespVO {

    private Long id;
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private Integer userType;
    private String clientId;
    private Long expiresTime;
}
