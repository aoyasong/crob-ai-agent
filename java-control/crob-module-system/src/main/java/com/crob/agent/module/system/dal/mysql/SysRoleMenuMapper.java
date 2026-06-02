package com.crob.agent.module.system.dal.mysql;

import com.crob.agent.framework.mybatis.core.mapper.BaseMapperX;
import com.crob.agent.module.system.dal.dataobject.SysRoleMenuDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;

public interface SysRoleMenuMapper extends BaseMapperX<SysRoleMenuDO> {

    default List<SysRoleMenuDO> selectListByRoleIds(Collection<Long> roleIds) {
        return selectList(new LambdaQueryWrapper<SysRoleMenuDO>()
                .in(SysRoleMenuDO::getRoleId, roleIds));
    }
}
