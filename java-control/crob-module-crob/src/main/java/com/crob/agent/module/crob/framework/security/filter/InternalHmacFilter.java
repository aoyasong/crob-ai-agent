package com.crob.agent.module.crob.framework.security.filter;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalHmacFilter extends OncePerRequestFilter {

    private final StringRedisTemplate stringRedisTemplate;
    private final String secret;
    private final Duration window;

    public InternalHmacFilter(
            StringRedisTemplate stringRedisTemplate, String secret, Duration window) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.secret = secret;
        this.window = window;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");
        if (timestamp == null || nonce == null || signature == null) {
            writeError(response, 401, "E_HMAC_MISSING");
            return;
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            writeError(response, 401, "E_HMAC_INVALID");
            return;
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > window.toMillis()) {
            writeError(response, 401, "E_HMAC_EXPIRED");
            return;
        }
        Boolean ok =
                stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent("crob:nonce:internal:" + nonce, "1", window.plusSeconds(30));
        if (ok == null || !ok) {
            writeError(response, 401, "E_HMAC_REPLAY");
            return;
        }

        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request, body);

        String bodySha256 = DigestUtil.sha256Hex(body);
        String canonical =
                timestamp
                        + "\n"
                        + request.getMethod()
                        + "\n"
                        + request.getRequestURI()
                        + "\n"
                        + bodySha256;
        HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        String expected = hmac.digestHex(canonical, StandardCharsets.UTF_8);
        if (!expected.equalsIgnoreCase(signature)) {
            writeError(response, 401, "E_HMAC_INVALID");
            return;
        }

        filterChain.doFilter(wrapped, response);
    }

    private void writeError(HttpServletResponse response, int status, String code)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\"}");
    }
}
