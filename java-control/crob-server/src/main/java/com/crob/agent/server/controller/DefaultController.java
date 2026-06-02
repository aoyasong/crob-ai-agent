package com.crob.agent.server.controller;

import com.crob.agent.framework.common.pojo.CommonResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefaultController {

    @RequestMapping(value = {"/test"})
    public CommonResult<String> test() {
        return CommonResult.success("ok");
    }
}
