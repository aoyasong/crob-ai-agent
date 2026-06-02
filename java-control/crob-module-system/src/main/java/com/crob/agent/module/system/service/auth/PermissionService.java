package com.crob.agent.module.system.service.auth;

import com.crob.agent.module.system.dal.dataobject.SysMenuDO;
import com.crob.agent.module.system.dal.dataobject.SysRoleDO;
import com.crob.agent.module.system.dal.dataobject.SysRoleMenuDO;
import com.crob.agent.module.system.dal.dataobject.SysUserRoleDO;
import com.crob.agent.module.system.dal.mysql.SysMenuMapper;
import com.crob.agent.module.system.dal.mysql.SysRoleMapper;
import com.crob.agent.module.system.dal.mysql.SysRoleMenuMapper;
import com.crob.agent.module.system.dal.mysql.SysUserRoleMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    @Resource
    private SysUserRoleMapper userRoleMapper;

    @Resource
    private SysRoleMapper roleMapper;

    @Resource
    private SysRoleMenuMapper roleMenuMapper;

    @Resource
    private SysMenuMapper menuMapper;

    public Set<String> getUserRoleCodes(Long userId) {
        List<SysUserRoleDO> userRoles = userRoleMapper.selectListByUserId(userId);
        if (userRoles.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRoleDO::getRoleId)
                .collect(Collectors.toList());
        List<SysRoleDO> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream()
                .filter(r -> r.getStatus() == null || r.getStatus() == 0)
                .map(SysRoleDO::getCode)
                .collect(Collectors.toSet());
    }

    public Set<String> getUserPermissions(Long userId) {
        Set<String> roleCodes = getUserRoleCodes(userId);
        if (roleCodes.contains("super_admin")) {
            return Set.of("*:*:*");
        }
        List<SysUserRoleDO> userRoles = userRoleMapper.selectListByUserId(userId);
        if (userRoles.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRoleDO::getRoleId)
                .collect(Collectors.toList());
        List<SysRoleMenuDO> roleMenus = roleMenuMapper.selectListByRoleIds(roleIds);
        if (roleMenus.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenuDO::getMenuId)
                .collect(Collectors.toList());
        List<SysMenuDO> menus = menuMapper.selectBatchIds(menuIds);
        return menus.stream()
                .filter(m -> m.getPermission() != null && !m.getPermission().isEmpty())
                .map(SysMenuDO::getPermission)
                .collect(Collectors.toSet());
    }

    public List<SysMenuDO> getUserMenus(Long userId) {
        Set<String> roleCodes = getUserRoleCodes(userId);
        List<SysMenuDO> allMenus;
        if (roleCodes.contains("super_admin")) {
            allMenus = menuMapper.selectList(null);
        } else {
            List<SysUserRoleDO> userRoles = userRoleMapper.selectListByUserId(userId);
            if (userRoles.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRoleDO::getRoleId)
                    .collect(Collectors.toList());
            List<SysRoleMenuDO> roleMenus = roleMenuMapper.selectListByRoleIds(roleIds);
            if (roleMenus.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> menuIds = roleMenus.stream()
                    .map(SysRoleMenuDO::getMenuId)
                    .collect(Collectors.toList());
            allMenus = menuMapper.selectBatchIds(menuIds);
        }
        // status=0 启用, status=1 停用, status=2 删除. 只排除已删除的菜单
        return allMenus.stream()
                .filter(m -> m.getStatus() == null || m.getStatus() != 2)
                .collect(Collectors.toList());
    }
}
