package com.crob.agent.module.system.controller.admin.menu;

import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.module.system.dal.dataobject.SysMenuDO;
import com.crob.agent.module.system.dal.mysql.SysMenuMapper;
import com.crob.agent.module.system.framework.security.core.SecurityFrameworkUtils;
import com.crob.agent.module.system.service.auth.MenuTreeBuilder;
import com.crob.agent.module.system.service.auth.PermissionService;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/menu")
public class SysMenuController {

    @Resource
    private SysMenuMapper menuMapper;

    @Resource
    private PermissionService permissionService;

    @Resource
    private MenuTreeBuilder menuTreeBuilder;

    // ========= 树形列表（供前端动态路由） =========
    @GetMapping("/list")
    public CommonResult<List<MenuTreeBuilder.MenuNode>> list() {
        try {
            Long userId = SecurityFrameworkUtils.getLoginUserId();
            if (userId == null) {
                return CommonResult.error(401, "未登录");
            }
            List<SysMenuDO> flatMenus = permissionService.getUserMenus(userId);
            List<MenuTreeBuilder.MenuNode> tree = menuTreeBuilder.buildTree(flatMenus);
            return CommonResult.success(tree);
        } catch (Exception e) {
            return CommonResult.error(500, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // ========= 简单列表 =========
    @GetMapping("/simple-list")
    public CommonResult<List<SysMenuDO>> simpleList() {
        return CommonResult.success(menuMapper.selectList(null));
    }

    // ========= CRUD =========
    @GetMapping("/get")
    public CommonResult<SysMenuDO> get(@RequestParam Long id) {
        return CommonResult.success(menuMapper.selectById(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@RequestBody SysMenuDO menu) {
        menuMapper.insert(menu);
        return CommonResult.success(menu.getId());
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@RequestBody SysMenuDO menu) {
        menuMapper.updateById(menu);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        menuMapper.deleteById(id);
        return CommonResult.success(true);
    }
}
