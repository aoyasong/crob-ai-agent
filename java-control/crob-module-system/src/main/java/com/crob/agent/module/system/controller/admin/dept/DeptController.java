package com.crob.agent.module.system.controller.admin.dept;

import static com.crob.agent.framework.common.pojo.CommonResult.success;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.module.system.dal.dataobject.SysDeptDO;
import com.crob.agent.module.system.dal.mysql.SysDeptMapper;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/dept")
public class DeptController {

    @Resource
    private SysDeptMapper deptMapper;

    @GetMapping("/simple-list")
    public CommonResult<List<SysDeptDO>> simpleList() {
        return CommonResult.success(deptMapper.selectList(null));
    }

    @GetMapping("/list")
    public CommonResult<List<SysDeptDO>> list(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "status", required = false) Integer status) {
        LambdaQueryWrapper<SysDeptDO> q = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            q.like(SysDeptDO::getName, name);
        }
        if (status != null) {
            q.eq(SysDeptDO::getStatus, status);
        }
        q.orderByAsc(SysDeptDO::getSort);
        return CommonResult.success(deptMapper.selectList(q));
    }

    @GetMapping("/get")
    public CommonResult<SysDeptDO> get(@RequestParam("id") Long id) {
        return CommonResult.success(deptMapper.selectById(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@RequestBody SysDeptDO dept) {
        deptMapper.insert(dept);
        return CommonResult.success(dept.getId());
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@RequestBody SysDeptDO dept) {
        deptMapper.updateById(dept);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        deptMapper.deleteById(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete-list")
    public CommonResult<Boolean> deleteList(@RequestParam("ids") String ids) {
        for (String s : ids.split(",")) {
            deptMapper.deleteById(Long.valueOf(s.trim()));
        }
        return CommonResult.success(true);
    }
}
