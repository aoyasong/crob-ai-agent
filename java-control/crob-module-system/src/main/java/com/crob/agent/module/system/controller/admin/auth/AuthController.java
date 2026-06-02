package com.crob.agent.module.system.controller.admin.auth;

import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.crob.agent.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.crob.agent.module.system.service.auth.AuthService;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/system/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/login")
    public CommonResult<AuthLoginRespVO> login(@RequestBody @Valid AuthLoginReqVO body) {
        return authService.login(body.getUsername(), body.getPassword());
    }

    @PostMapping("/logout")
    public CommonResult<Boolean> logout() {
        return CommonResult.success(true);
    }

    @PostMapping("/refresh-token")
    public CommonResult<AuthLoginRespVO> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return authService.refreshToken(refreshToken);
    }
}
