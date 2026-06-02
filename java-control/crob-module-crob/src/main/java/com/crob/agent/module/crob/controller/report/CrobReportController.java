package com.crob.agent.module.crob.controller.report;

import cn.hutool.core.util.StrUtil;
import com.crob.agent.module.crob.dal.dataobject.attempt.CrobAttemptDO;
import com.crob.agent.module.crob.dal.dataobject.result.CrobTaskResultDO;
import com.crob.agent.module.crob.dal.mysql.attempt.CrobAttemptMapper;
import com.crob.agent.module.crob.dal.mysql.result.CrobTaskResultMapper;
import javax.annotation.Resource;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CrobReportController {

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().build();

    @Resource private CrobTaskResultMapper taskResultMapper;
    @Resource private CrobAttemptMapper attemptMapper;

    @GetMapping(value = "/tasks/{taskId}/report", produces = MediaType.TEXT_HTML_VALUE)
    public String report(
            @PathVariable("taskId") Long taskId, @RequestParam("attempt_id") Long attemptId) {
        CrobAttemptDO attempt = attemptMapper.selectById(attemptId);
        if (attempt == null
                || !taskId.equals(attempt.getTaskId())
                || !"SUCCEEDED".equals(attempt.getStatus())) {
            return htmlPage("Report not available");
        }
        CrobTaskResultDO result = taskResultMapper.select(taskId, attemptId);
        if (result == null || StrUtil.isBlank(result.getReportMd())) {
            return htmlPage("Report not found");
        }
        String body = HTML_RENDERER.render(MARKDOWN_PARSER.parse(result.getReportMd()));
        return htmlPage(body);
    }

    private String htmlPage(String body) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"/>"
                + "<style>body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;max-width:900px;margin:0 auto;padding:20px;line-height:1.6}pre{background:#f4f4f4;padding:12px;border-radius:4px;overflow-x:auto}code{background:#f4f4f4;padding:2px 4px;border-radius:2px}table{border-collapse:collapse;width:100%}td,th{border:1px solid #ddd;padding:8px;text-align:left}th{background:#f0f0f0}</style>"
                + "</head><body>"
                + body
                + "</body></html>";
    }
}
