package com.crob.agent.module.system.dal.mysql;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crob.agent.framework.mybatis.core.mapper.BaseMapperX;
import com.crob.agent.module.system.dal.dataobject.SysUserDO;

public interface SysUserMapper extends BaseMapperX<SysUserDO> {

    default SysUserDO selectByUsername(String username) {
        return selectOne(
                new LambdaQueryWrapper<SysUserDO>().eq(SysUserDO::getUsername, username));
    }
}
