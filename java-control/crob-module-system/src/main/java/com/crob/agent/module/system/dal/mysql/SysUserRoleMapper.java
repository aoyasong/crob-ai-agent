package com.crob.agent.module.system.dal.mysql;

import com.crob.agent.framework.mybatis.core.mapper.BaseMapperX;
import com.crob.agent.module.system.dal.dataobject.SysUserRoleDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;

public interface SysUserRoleMapper extends BaseMapperX<SysUserRoleDO> {

    default List<SysUserRoleDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<SysUserRoleDO>()
                .eq(SysUserRoleDO::getUserId, userId));
    }
}
