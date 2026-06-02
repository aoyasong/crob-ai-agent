package com.crob.agent.module.system.framework.security.filter;

import com.crob.agent.module.system.dal.dataobject.SysUserDO;
import com.crob.agent.module.system.dal.mysql.SysUserMapper;
import com.crob.agent.module.system.framework.jwt.JwtUtils;
import com.crob.agent.module.system.framework.security.core.LoginUser;
import com.crob.agent.module.system.framework.security.core.SecurityFrameworkUtils;
import java.io.IOException;
import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private SysUserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        String token = extractToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            try {
                Long userId = jwtUtils.getUserId(token);
                SysUserDO user = userMapper.selectById(userId);
                if (user != null && (user.getStatus() == null || user.getStatus() == 0)) {
                    LoginUser loginUser = new LoginUser();
                    loginUser.setId(user.getId());
                    loginUser.setUsername(user.getUsername());
                    loginUser.setNickname(user.getNickname());
                    loginUser.setTenantId(user.getTenantId());
                    SecurityFrameworkUtils.setLoginUser(loginUser);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    loginUser, null, loginUser.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // Token invalid or user not found, leave unauthenticated
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityFrameworkUtils.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
