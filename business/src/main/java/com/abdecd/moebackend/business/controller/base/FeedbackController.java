package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.lib.RateLimiter;
import com.abdecd.moebackend.business.pojo.dto.feedback.AddFeedbackDTO;
import com.abdecd.moebackend.business.service.feedback.FeedbackService;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private RateLimiter rateLimiter;

    @Operation(summary = "添加反馈")
    @PostMapping("/add")
    public Result<String> addFeedback(
            @RequestBody @Valid AddFeedbackDTO addFeedbackDTO,
            HttpServletRequest request
    ) {
        if (rateLimiter.isRateLimited(
                RedisConstant.LIMIT_FEEDBACK_ADD + request.getHeader("X-Real-IP"),
                RedisConstant.LIMIT_FEEDBACK_ADD_CNT,
                RedisConstant.LIMIT_FEEDBACK_ADD_RESET_TIME,
                TimeUnit.SECONDS
        )) throw new BaseException(MessageConstant.RATE_LIMIT);
        feedbackService.addFeedback(addFeedbackDTO);
        return Result.success();
    }
}
