package com.crob.agent.module.system.controller.admin.permission;

import com.crob.agent.framework.common.pojo.CommonResult;
import java.util.Collections;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/permission")
public class SysPermissionController {

    @GetMapping("/list-role-menus")
    public CommonResult<List<Long>> listRoleMenus() {
        return CommonResult.success(Collections.emptyList());
    }
}
