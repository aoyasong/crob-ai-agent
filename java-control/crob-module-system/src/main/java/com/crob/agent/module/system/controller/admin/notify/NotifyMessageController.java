package com.crob.agent.module.system.controller.admin.notify;

import com.crob.agent.framework.common.pojo.CommonResult;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/notify-message")
public class NotifyMessageController {

    @GetMapping("/get-unread-count")
    public CommonResult<Long> getUnreadCount() {
        return CommonResult.success(0L);
    }

    @GetMapping("/get-unread-list")
    public CommonResult<java.util.List<Object>> getUnreadList() {
        return CommonResult.success(java.util.Collections.emptyList());
    }

    @GetMapping("/page")
    public CommonResult<Map<String, Object>> page() {
        return CommonResult.success(Map.of("list", java.util.Collections.emptyList(), "total", 0));
    }

    @GetMapping("/my-page")
    public CommonResult<Map<String, Object>> myPage() {
        return CommonResult.success(Map.of("list", java.util.Collections.emptyList(), "total", 0));
    }
}
