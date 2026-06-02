package com.crob.agent.module.system.controller.admin.dict;

import com.crob.agent.framework.common.pojo.CommonResult;
import java.util.Collections;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/dict-type")
public class DictTypeController {

    @GetMapping("/simple-list")
    public CommonResult<List<Object>> simpleList() {
        return CommonResult.success(Collections.emptyList());
    }
}
