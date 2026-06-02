package com.crob.agent.module.system.framework.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "crob.jwt")
@Component
@Data
public class JwtProperties {

    private String secret;
    private long accessTokenExpireMinutes = 1440;
    private long refreshTokenExpireMinutes = 43200;
}
