package com.crob.agent.module.system.framework.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    @Resource
    private JwtProperties jwtProperties;

    public String createAccessToken(Long userId, String username) {
        return createToken(userId, username, "access",
                jwtProperties.getAccessTokenExpireMinutes() * 60 * 1000);
    }

    public String createRefreshToken(Long userId, String username) {
        return createToken(userId, username, "refresh",
                jwtProperties.getRefreshTokenExpireMinutes() * 60 * 1000);
    }

    private String createToken(Long userId, String username, String tokenType, long expireMs) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("tokenType", tokenType)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMs))
                .signWith(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    public String getTokenType(String token) {
        return parseToken(token).get("tokenType", String.class);
    }
}
