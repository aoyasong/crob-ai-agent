package com.crob.agent.module.system.service.auth;

import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.crob.agent.module.system.dal.dataobject.SysUserDO;
import com.crob.agent.module.system.dal.mysql.SysUserMapper;
import com.crob.agent.module.system.framework.jwt.JwtUtils;
import io.jsonwebtoken.Claims;
import javax.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Resource
    private SysUserMapper userMapper;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private PasswordEncoder passwordEncoder;

    public CommonResult<AuthLoginRespVO> login(String username, String password) {
        if (username == null || password == null) {
            return CommonResult.error(400, "用户名或密码不能为空");
        }
        SysUserDO user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return CommonResult.error(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            return CommonResult.error(403, "用户已被禁用");
        }
        String accessToken = jwtUtils.createAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.createRefreshToken(user.getId(), user.getUsername());
        Claims claims = jwtUtils.parseToken(accessToken);

        AuthLoginRespVO resp = new AuthLoginRespVO()
                .setId(user.getId())
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setUserId(user.getId())
                .setUserType(1)
                .setClientId("default")
                .setExpiresTime(claims.getExpiration().getTime());
        return CommonResult.success(resp);
    }

    public CommonResult<AuthLoginRespVO> refreshToken(String refreshToken) {
        if (refreshToken == null || !jwtUtils.validateToken(refreshToken)) {
            return CommonResult.error(401, "刷新令牌无效或已过期");
        }
        String tokenType = jwtUtils.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            return CommonResult.error(401, "无效的刷新令牌类型");
        }
        try {
            Long userId = jwtUtils.getUserId(refreshToken);
            SysUserDO user = userMapper.selectById(userId);
            if (user == null || (user.getStatus() != null && user.getStatus() != 0)) {
                return CommonResult.error(401, "用户不存在或已禁用");
            }
            String newAccessToken = jwtUtils.createAccessToken(userId, user.getUsername());
            String newRefreshToken = jwtUtils.createRefreshToken(userId, user.getUsername());
            Claims claims = jwtUtils.parseToken(newAccessToken);

            AuthLoginRespVO resp = new AuthLoginRespVO()
                    .setId(userId)
                    .setAccessToken(newAccessToken)
                    .setRefreshToken(newRefreshToken)
                    .setUserId(userId)
                    .setUserType(1)
                    .setClientId("default")
                    .setExpiresTime(claims.getExpiration().getTime());
            return CommonResult.success(resp);
        } catch (Exception e) {
            return CommonResult.error(401, "刷新令牌处理失败");
        }
    }
}
