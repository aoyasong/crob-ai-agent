package com.crob.agent.module.crob.service.task.executor;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.crob.agent.framework.common.util.json.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CrobExecutorClientImpl implements CrobExecutorClient {

    @Value("${PYTHON_EXECUTOR_BASE_URL:}")
    private String executorBaseUrl;

    @Value("${INTERNAL_HMAC_SECRET:}")
    private String hmacSecret;

    @Resource private RestTemplate restTemplate;

    @Override
    public void execute(
            Long taskId,
            Long attemptId,
            String scenario,
            Map<String, Object> inputs,
            Map<String, Object> effectiveConstraints) {
        Map<String, Object> bodyObj = new HashMap<>();
        bodyObj.put("task_id", taskId);
        bodyObj.put("attempt_id", attemptId);
        bodyObj.put("scenario", scenario);
        bodyObj.put("inputs", inputs);
        bodyObj.put("effective_constraints_json", effectiveConstraints);

        String path = "/internal/execute";
        String url = executorBaseUrl + path;
        String body = JsonUtils.toJsonString(bodyObj);

        long ts = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String bodySha256 = DigestUtil.sha256Hex(body);
        String canonical = ts + "\n" + "POST" + "\n" + path + "\n" + bodySha256;
        HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, hmacSecret.getBytes(StandardCharsets.UTF_8));
        String signature = hmac.digestHex(canonical, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Timestamp", String.valueOf(ts));
        headers.set("X-Nonce", nonce);
        headers.set("X-Signature", signature);

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }
}
