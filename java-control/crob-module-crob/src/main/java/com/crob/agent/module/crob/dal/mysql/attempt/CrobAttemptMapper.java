package com.crob.agent.module.crob.dal.mysql.attempt;

import com.crob.agent.framework.mybatis.core.mapper.BaseMapperX;
import com.crob.agent.module.crob.dal.dataobject.attempt.CrobAttemptDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrobAttemptMapper extends BaseMapperX<CrobAttemptDO> {}
