package com.crob.agent.module.system.controller.admin.auth;

import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.module.system.dal.dataobject.SysMenuDO;
import com.crob.agent.module.system.dal.dataobject.SysUserDO;
import com.crob.agent.module.system.dal.mysql.SysUserMapper;
import com.crob.agent.module.system.framework.security.core.SecurityFrameworkUtils;
import com.crob.agent.module.system.service.auth.MenuTreeBuilder;
import com.crob.agent.module.system.service.auth.PermissionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/auth")
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    @Resource
    private MenuTreeBuilder menuTreeBuilder;

    @Resource
    private SysUserMapper userMapper;

    @GetMapping("/get-permission-info")
    public CommonResult<Map<String, Object>> getPermissionInfo() {
        try {
            Long userId = SecurityFrameworkUtils.getLoginUserId();
            if (userId == null) {
                return CommonResult.error(401, "未登录");
            }
            SysUserDO user = userMapper.selectById(userId);
            if (user == null) {
                return CommonResult.error(500, "用户不存在: " + userId);
            }
            Set<String> roles = permissionService.getUserRoleCodes(userId);
            Set<String> permissions = permissionService.getUserPermissions(userId);
            List<SysMenuDO> flatMenus = permissionService.getUserMenus(userId);
            List<MenuTreeBuilder.MenuNode> menus = menuTreeBuilder.buildTree(flatMenus);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("nickname", user.getNickname());
            userMap.put("deptId", user.getDeptId());
            userMap.put("email", user.getEmail());
            userMap.put("mobile", user.getMobile());
            userMap.put("sex", user.getSex());
            userMap.put("avatar", user.getAvatar());
            userMap.put("loginIp", user.getLoginIp());
            userMap.put("loginDate", user.getLoginDate());

            Map<String, Object> result = new HashMap<>();
            result.put("user", userMap);
            result.put("roles", roles);
            result.put("permissions", permissions);
            result.put("menus", menus);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.error(500, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
