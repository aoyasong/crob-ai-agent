package com.crob.agent.module.crob.controller.admin.task;

import static com.crob.agent.framework.common.pojo.CommonResult.success;

import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobAttemptRespVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskCreateReqVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskInputsReqVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskRerunReqVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskRespVO;
import com.crob.agent.module.crob.controller.admin.task.vo.CrobTaskStreamRespVO;
import com.crob.agent.module.crob.service.task.CrobTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Tag(name = "任务平台 - 任务")
@RestController
@RequestMapping("/api/tasks")
public class CrobTaskController {

    @Resource private CrobTaskService taskService;

    @PostMapping
    @Operation(summary = "创建任务")
    public CommonResult<CrobTaskRespVO> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CrobTaskCreateReqVO reqVO) {
        return success(taskService.createTask(reqVO, idempotencyKey));
    }

    @PostMapping("/{taskId}/inputs")
    @Operation(summary = "补参")
    public CommonResult<CrobTaskRespVO> inputs(
            @PathVariable("taskId") Long taskId, @Valid @RequestBody CrobTaskInputsReqVO reqVO) {
        return success(taskService.submitInputs(taskId, reqVO));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "查询任务")
    public CommonResult<CrobTaskRespVO> get(@PathVariable("taskId") Long taskId) {
        return success(taskService.getTask(taskId));
    }

    @GetMapping("/{taskId}/attempts")
    @Operation(summary = "查询任务的 attempts")
    public CommonResult<List<CrobAttemptRespVO>> attempts(@PathVariable("taskId") Long taskId) {
        return success(taskService.listAttempts(taskId));
    }

    @PostMapping("/{taskId}/cancel")
    @Operation(summary = "取消任务（取消当前 attempt）")
    public CommonResult<CrobTaskRespVO> cancel(
            @PathVariable("taskId") Long taskId,
            @RequestParam(value = "attempt_id", required = false) Long attemptId) {
        return success(taskService.cancel(taskId, attemptId));
    }

    @PostMapping("/{taskId}/rerun")
    @Operation(summary = "重跑任务（生成新 attempt）")
    public CommonResult<CrobTaskRespVO> rerun(
            @PathVariable("taskId") Long taskId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) CrobTaskRerunReqVO reqVO) {
        return success(taskService.rerun(taskId, reqVO, idempotencyKey));
    }

    @GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式输出（SSE）")
    public Flux<CommonResult<CrobTaskStreamRespVO>> stream(
            @PathVariable("taskId") Long taskId, @RequestParam("attempt_id") Long attemptId) {
        return taskService.stream(taskId, attemptId);
    }
}
