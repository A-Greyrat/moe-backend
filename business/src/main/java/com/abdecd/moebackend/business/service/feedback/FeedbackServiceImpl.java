package com.abdecd.moebackend.business.service.feedback;

import com.abdecd.moebackend.business.dao.entity.Feedback;
import com.abdecd.moebackend.business.dao.mapper.FeedbackMapper;
import com.abdecd.moebackend.business.pojo.dto.feedback.AddFeedbackDTO;
import com.abdecd.moebackend.business.pojo.dto.feedback.HandleFeedbackDTO;
import com.abdecd.moebackend.business.pojo.vo.feedback.FeedbackVO;
import com.abdecd.moebackend.common.constant.StatusConstant;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Override
    public Long addFeedback(AddFeedbackDTO addFeedbackDTO) {
        Feedback feedback = new Feedback();
        BeanUtils.copyProperties(addFeedbackDTO, feedback);
        feedback.setTimestamp(LocalDateTime.now());
        feedback.setStatus(Integer.valueOf(StatusConstant.ENABLE));
        feedbackMapper.insert(feedback);
        return feedback.getId();
    }

    @Override
    public Page<FeedbackVO> getFeedbacks(int page, int pageSize, String email, String content) {
        Page<FeedbackVO> feedbackPage = new Page<>(page, pageSize);
        return feedbackMapper.selectFeedbacks(feedbackPage, email, content);
    }

    @Override
    public boolean handleFeedback(HandleFeedbackDTO handleFeedbackDTO) {
        Feedback feedback = feedbackMapper.selectById(handleFeedbackDTO.getId());
        if (feedback == null) {
            return false;
        }
        feedback.setStatus(Integer.valueOf(StatusConstant.DISABLE));
        return feedbackMapper.updateById(feedback) == 1;
    }

    @Override
    public void deleteFeedback(Long[] ids) {
        feedbackMapper.deleteBatchIds(Arrays.asList(ids));
    }
}
