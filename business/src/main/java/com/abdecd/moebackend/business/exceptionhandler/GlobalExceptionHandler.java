package com.abdecd.moebackend.business.exceptionhandler;

import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(BaseException ex) {
        log.info("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 参数校验 针对json
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(HandlerMethodValidationException ex) {
        log.info("参数校验失败：{}", ex.toString());
        return Result.error("参数校验失败: " + ex.getAllErrors().getFirst().getDefaultMessage());
    }
    /**
     * 参数校验 针对除json外的
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(MethodArgumentNotValidException ex) {
        log.info("参数校验失败：{}", ex.toString());
        return Result.error("参数校验失败: " + ex.getAllErrors().getFirst().getDefaultMessage());
    }
    @ExceptionHandler
    public Result<String> exceptionHandler(MethodArgumentTypeMismatchException ex) {
        log.info("参数类型错误：{}", ex.toString());
//        return Result.error("参数类型错误: " + ex.getMessage());
        return Result.error("参数类型错误");
    }
    @ExceptionHandler
    public Result<String> exceptionHandler(MissingServletRequestParameterException ex) {
        log.info("参数缺失：{}", ex.toString());
//        return Result.error("参数缺失: " + ex.getMessage());
        return Result.error("参数缺失");
    }

    /**
     * 表单异常
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(MultipartException ex) {
        log.info("表单异常：{}", ex.toString());
        return Result.error(MessageConstant.MULTIPART_EXCEPTION);
    }

    /**
     * 处理数据库重复键异常
     */
    @ExceptionHandler
    public Result<String> sqlExceptionHandler(SQLIntegrityConstraintViolationException ex) {
        var msg = ex.getMessage();
        log.warn("数据库重复键：{}", msg);
        var code = msg.split(" ")[5];
        if (msg.contains("Duplicate entry"))
            return Result.error("重复的值：" + code.substring(code.lastIndexOf('.') + 1, code.length() - 1));
        else return Result.error(MessageConstant.UNKNOWN_ERROR);
    }

    @ExceptionHandler
    public Result<String> exceptionHandler(DataIntegrityViolationException ex) {
        log.warn("数据库操作异常：{}", ex.toString());
        return Result.error(MessageConstant.DB_ERROR);
    }

    /**
     * 实例对象不完整
     */
    @ExceptionHandler
    public Result<String> nullPointerExceptionHandler(NullPointerException ex) {
        log.error("空指针异常：{}", ex.toString());
        return Result.error(500, "信息不全");
    }
}
