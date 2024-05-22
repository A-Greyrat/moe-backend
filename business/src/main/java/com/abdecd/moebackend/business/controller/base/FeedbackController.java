package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.pojo.dto.feedback.AddFeedbackDTO;
import com.abdecd.moebackend.business.pojo.dto.feedback.HandleFeedbackDTO;
import com.abdecd.moebackend.business.pojo.vo.feedback.FeedbackVO;
import com.abdecd.moebackend.business.service.feedback.FeedbackService;
import com.abdecd.moebackend.common.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/backstage/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Operation(summary = "添加反馈")
    @PostMapping("/add")
    public Result<String> addFeedback(@RequestBody @Valid AddFeedbackDTO addFeedbackDTO) {
        Long feedbackId = feedbackService.addFeedback(addFeedbackDTO);
        return Result.success(feedbackId + "");
    }

    @Operation(summary = "查看反馈")
    @GetMapping
    public Result<Object> getFeedbacks(
            @RequestParam int page,
            @RequestParam int pageSize,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String content) {

        Page<FeedbackVO> feedbackPage = feedbackService.getFeedbacks(page, pageSize, email, content);
        return Result.success(Map.of(
                "total", feedbackPage.getTotal(),
                "records", feedbackPage.getRecords()
        ));
    }

    @Operation(summary = "处理反馈")
    @PostMapping("/handle")
    public Result<String> handleFeedback(@RequestBody @Valid HandleFeedbackDTO handleFeedbackDTO) {
        boolean success = feedbackService.handleFeedback(handleFeedbackDTO);
        if (success) {
            return Result.success("ok");
        } else {
            return Result.error("反馈处理失败");
        }
    }
}
