package com.crob.agent.module.system.controller.admin.tenant;

import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.module.system.dal.dataobject.SysTenantDO;
import com.crob.agent.module.system.dal.mysql.SysTenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/tenant")
public class TenantController {

    @Resource
    private SysTenantMapper tenantMapper;

    @GetMapping("/get-id-by-name")
    public CommonResult<Long> getTenantIdByName(@RequestParam String name) {
        SysTenantDO tenant = tenantMapper.selectOne(
                new LambdaQueryWrapper<SysTenantDO>()
                        .eq(SysTenantDO::getName, name)
                        .last("LIMIT 1"));
        if (tenant == null) {
            return CommonResult.error(404, "租户不存在");
        }
        return CommonResult.success(tenant.getId());
    }

    @GetMapping("/simple-list")
    public CommonResult<java.util.List<Map<String, Object>>> simpleList() {
        return CommonResult.success(java.util.Collections.emptyList());
    }

    @GetMapping("/get-by-website")
    public CommonResult<Map<String, Object>> getTenantByWebsite(@RequestParam String website) {
        SysTenantDO tenant = tenantMapper.selectOne(
                new LambdaQueryWrapper<SysTenantDO>()
                        .eq(SysTenantDO::getDomain, website)
                        .last("LIMIT 1"));
        if (tenant == null) {
            return CommonResult.error(404, "租户不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", tenant.getId());
        result.put("name", tenant.getName());
        return CommonResult.success(result);
    }
}
