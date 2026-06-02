package com.crob.agent.module.crob.framework.security.filter;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class InternalHmacFilterConfiguration {

    @Bean
    public FilterRegistrationBean<InternalHmacFilter> internalHmacFilterRegistration(
            StringRedisTemplate stringRedisTemplate,
            @Value("${INTERNAL_HMAC_SECRET:}") String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("INTERNAL_HMAC_SECRET is required");
        }
        FilterRegistrationBean<InternalHmacFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(
                new InternalHmacFilter(stringRedisTemplate, secret, Duration.ofSeconds(60)));
        registration.addUrlPatterns("/internal/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
