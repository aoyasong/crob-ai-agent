package com.crob.agent.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SuppressWarnings("SpringComponentScan")
@SpringBootApplication(scanBasePackages = {"${crob.info.base-package}.server", "${crob.info.base-package}.module"})
@MapperScan("${crob.info.base-package}.module.*.dal.mysql")
@EnableAsync
@EnableScheduling
public class CrobServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrobServerApplication.class, args);
    }

}
