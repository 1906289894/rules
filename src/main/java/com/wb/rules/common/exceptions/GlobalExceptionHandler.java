package com.wb.rules.common.exceptions;

import com.wb.rules.common.enums.RuleResultCode;
import com.wb.rules.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuleException.class)
    public R<Void> handleRuleException(final RuleException e) {
        log.error(e.getMessage(), e);
        return R.error(RuleResultCode.BUSINESS_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Validated 或 @Valid 触发的校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常: {}", errorMessage);
        return R.error(RuleResultCode.PARAM_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理数据绑定异常
     */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("数据绑定异常: {}", errorMessage);
        return R.error(RuleResultCode.PARAM_ERROR.getCode(), errorMessage);
    }


    /**
     * 处理参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("参数缺失: {}", e.getParameterName());
        return R.error(RuleResultCode.PARAM_ERROR.getCode(), "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getMessage());
        return R.error(RuleResultCode.PARAM_ERROR.getCode(), "参数类型错误: " + e.getMessage());
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("接口不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return R.error(RuleResultCode.NOT_FOUND);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public R<Void> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常:", e);
        return R.error(RuleResultCode.SYSTEM_ERROR.getCode(), "系统出现空指针异常");
    }

    /**
     * 处理所有未捕获的异常（兜底处理）
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return R.error(RuleResultCode.SYSTEM_ERROR);
    }
}
