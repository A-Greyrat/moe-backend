package com.abdecd.moebackend.business.exceptionhandler;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex 异常
     * @return Result
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(BaseException ex) {
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 处理数据库重复键异常
     * @param ex 异常
     * @return Result
     */
    @ExceptionHandler
    public Result<String> sqlExceptionHandler(SQLIntegrityConstraintViolationException ex) {
        var msg = ex.getMessage();
        log.error("异常信息：{}", msg);
        if (msg.contains("Duplicate entry"))
            return Result.error("重复的值："+msg.split(" ")[2]);
        else return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}