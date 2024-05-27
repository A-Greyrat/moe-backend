package com.abdecd.moebackend.business.exceptionhandler;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

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
        var code = msg.split(" ")[5];
        if (msg.contains("Duplicate entry"))
            return Result.error("重复的值：" + code.substring(code.lastIndexOf('.') + 1, code.length() - 1));
        else return Result.error(MessageConstant.UNKNOWN_ERROR);
    }

    /**
     * 处理参数校验异常
     * @param ex 异常
     * @return Result
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(MethodArgumentNotValidException ex) throws MethodArgumentNotValidException {
        log.warn("异常信息：{}", ex.getMessage());
        var err = ex.getBindingResult().getFieldError();
        if (err == null) throw ex;
        return Result.error(err.getField()
                + ": "
                + err.getDefaultMessage()
        );
//        return Result.error(err.getDefaultMessage());
    }

    /**
     * 实例对象不完整
     * @param ex 异常
     * @return Result
     */
    @ExceptionHandler
    public Result<String> nullPointerExceptionHandler(NullPointerException ex) {
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(500, "信息不全");
    }

    @ExceptionHandler
    public Result<String> exceptionHandler(Exception ex) {
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(500, MessageConstant.UNKNOWN_ERROR);
    }

    @ExceptionHandler
    public Result<String> exceptionHandler(MultipartException ex) {
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(MessageConstant.MULTIPART_EXCEPTION);
    }

    @ExceptionHandler
    public Result<String> exceptionHandler(IllegalArgumentException ex) {
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(MessageConstant.ARG_ERROR);
    }

    @ExceptionHandler
    public Result<String> exceptionHandler(DataIntegrityViolationException ex) {
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(MessageConstant.DB_ERROR);
    }
}
