package com.abdecd.moebackend.business.service.feedback;

import com.abdecd.moebackend.business.pojo.dto.feedback.AddFeedbackDTO;
import com.abdecd.moebackend.business.pojo.dto.feedback.HandleFeedbackDTO;
import com.abdecd.moebackend.business.pojo.vo.feedback.FeedbackVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface FeedbackService {
    Long addFeedback(AddFeedbackDTO addFeedbackDTO);
    Page<FeedbackVO> getFeedbacks(int page, int pageSize, String email, String content);
    boolean handleFeedback(HandleFeedbackDTO handleFeedbackDTO);

    void deleteFeedback(Long[] ids);
}
