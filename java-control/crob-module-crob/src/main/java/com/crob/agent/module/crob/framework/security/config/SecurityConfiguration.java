package com.crob.agent.module.crob.framework.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@Order(99)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors()
                .and()
                .csrf()
                .disable()
                .authorizeRequests()
                .antMatchers("/test")
                .permitAll()
                .antMatchers("/api/tasks/**")
                .permitAll()
                .antMatchers("/tasks/**")
                .permitAll()
                .antMatchers("/internal/**")
                .permitAll()
                .antMatchers("/actuator/**")
                .permitAll()
                .anyRequest()
                .permitAll();
    }
}
