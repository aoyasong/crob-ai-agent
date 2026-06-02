package com.crob.agent.module.crob.service.task;

import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobAttemptRespVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskCreateReqVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskInputsReqVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskRerunReqVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskRespVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskStreamRespVO;
import java.util.List;
import reactor.core.publisher.Flux;

public interface CrobTaskService {

    CrobTaskRespVO createTask(CrobTaskCreateReqVO reqVO, String idempotencyKey);

    CrobTaskRespVO submitInputs(Long taskId, CrobTaskInputsReqVO reqVO);

    CrobTaskRespVO getTask(Long taskId);

    List<CrobAttemptRespVO> listAttempts(Long taskId);

    CrobTaskRespVO cancel(Long taskId, Long attemptId);

    CrobTaskRespVO rerun(Long taskId, CrobTaskRerunReqVO reqVO, String idempotencyKey);

    Flux<CommonResult<CrobTaskStreamRespVO>> stream(Long taskId, Long attemptId);
}
