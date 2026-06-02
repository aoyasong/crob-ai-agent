package com.crob.agent.framework.mybatis.core.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;

public interface BaseMapperX<T> extends BaseMapper<T> {

    default List<T> selectAll() {
        return selectList(new LambdaQueryWrapper<>());
    }
}
