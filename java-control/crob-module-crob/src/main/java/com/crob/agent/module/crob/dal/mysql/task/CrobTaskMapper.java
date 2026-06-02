package com.crob.agent.module.crob.dal.mysql.task;

import com.crob.agent.framework.mybatis.core.mapper.BaseMapperX;
import com.crob.agent.module.crob.dal.dataobject.task.CrobTaskDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrobTaskMapper extends BaseMapperX<CrobTaskDO> {}
