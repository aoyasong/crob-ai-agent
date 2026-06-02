package com.crob.agent.module.system.controller.admin.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.module.system.dal.dataobject.SysRoleDO;
import com.crob.agent.module.system.dal.mysql.SysRoleMapper;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/role")
public class RoleController {

    @Resource
    private SysRoleMapper roleMapper;

    @GetMapping("/page")
    public CommonResult<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code) {
        Page<SysRoleDO> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SysRoleDO> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) wrapper.like(SysRoleDO::getName, name);
        if (code != null && !code.isEmpty()) wrapper.like(SysRoleDO::getCode, code);
        IPage<SysRoleDO> doPage = roleMapper.selectPage(page, wrapper);
        return CommonResult.success(Map.of("list", doPage.getRecords(), "total", doPage.getTotal()));
    }

    @GetMapping("/simple-list")
    public CommonResult<List<SysRoleDO>> simpleList() {
        return CommonResult.success(roleMapper.selectList(
                new LambdaQueryWrapper<SysRoleDO>().eq(SysRoleDO::getStatus, 0)));
    }

    @GetMapping("/get")
    public CommonResult<SysRoleDO> get(@RequestParam Long id) {
        return CommonResult.success(roleMapper.selectById(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@RequestBody SysRoleDO role) {
        roleMapper.insert(role);
        return CommonResult.success(role.getId());
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@RequestBody SysRoleDO role) {
        roleMapper.updateById(role);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        roleMapper.deleteById(id);
        return CommonResult.success(true);
    }
}
