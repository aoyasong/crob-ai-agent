package com.crob.agent.module.crob.dal.mysql.event;

import com.crob.agent.framework.mybatis.core.mapper.BaseMapperX;
import com.crob.agent.module.crob.dal.dataobject.event.CrobUsageEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrobUsageEventMapper extends BaseMapperX<CrobUsageEventDO> {}
