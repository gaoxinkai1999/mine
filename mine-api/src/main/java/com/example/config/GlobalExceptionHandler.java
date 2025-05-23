package com.example.config;

import com.example.exception.ErrorResponse;
import com.example.exception.MyException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException; // 导入超时异常类

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理参数校验失败的异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors();
        String errorMessage = errors.isEmpty() ?
                "请求参数错误" : errors.getFirst().getDefaultMessage();
        log.warn("参数校验失败: {}", errorMessage);
        return new ErrorResponse(errorMessage);
    }

    /**
     * 处理单个参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException ex) {
        String errorMessage = ex.getMessage();
        log.warn("参数验证失败: {}", errorMessage);
        return new ErrorResponse(errorMessage);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(MyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBusinessException(MyException e) {
        log.warn("业务异常: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception e) {
        // 检查是否是 SSE 请求超时异常
        if (e instanceof AsyncRequestTimeoutException) {
            log.warn("忽略 SSE 请求超时异常: {}", e.getMessage());
            // 对于 SSE 超时，不返回自定义错误体，让 SseEmitter 内部处理完成或错误回调
            return null; 
        }
        
        // 处理其他所有未知异常
        log.error("系统异常: ", e);
        return new ErrorResponse("系统异常,请稍后重试");
    }
}
