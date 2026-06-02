package com.crob.agent.module.system.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置：将 Long 类型序列化为字符串，避免 JavaScript Number 精度丢失。
 * 直接注入 Spring Boot 自动配置的 ObjectMapper 并添加模块，不干扰 Builder 链。
 */
@Configuration
public class JacksonConfig {

    @Resource
    private ObjectMapper objectMapper;

    @PostConstruct
    public void registerLongToStringSerializer() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);
    }
}
