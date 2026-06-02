package com.crob.agent.framework.common.exception;

import com.crob.agent.framework.common.exception.util.ServiceExceptionUtil;
import com.crob.agent.framework.common.pojo.CommonResult;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceExceptionUtil.ServiceException.class)
    public CommonResult<?> handleServiceException(ServiceExceptionUtil.ServiceException ex,
            HttpServletRequest request) {
        log.warn("ServiceException at {} {}: code={}, msg={}",
                request.getMethod(), request.getRequestURI(), ex.getCode(), ex.getMessage());
        return CommonResult.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<?> handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("Unknown error at {} {}", request.getMethod(), request.getRequestURI(), ex);
        return CommonResult.error(500, "Internal Server Error: " + ex.getMessage());
    }
}
