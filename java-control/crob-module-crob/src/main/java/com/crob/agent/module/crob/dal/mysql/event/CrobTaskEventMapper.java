package com.crob.agent.module.crob.dal.mysql.event;

import com.crob.agent.framework.mybatis.core.mapper.BaseMapperX;
import com.crob.agent.module.crob.dal.dataobject.event.CrobTaskEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrobTaskEventMapper extends BaseMapperX<CrobTaskEventDO> {}
