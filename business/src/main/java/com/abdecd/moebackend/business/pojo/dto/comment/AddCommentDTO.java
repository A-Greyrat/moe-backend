package com.abdecd.moebackend.business.pojo.dto.comment;

import com.abdecd.moebackend.business.common.util.SensitiveUtils;
import com.abdecd.moebackend.business.dao.entity.UserComment;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.common.constant.DTOConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class AddCommentDTO {
    @NotNull
    private Long videoId;
    @NotNull
    @Schema(description = "目标评论id，对视频写-1")
    private Long toId;
    @NotBlank
    @Length(min = 1, max = DTOConstant.COMMENT_LENGTH_MAX)
    private String content;

    public UserComment toEntity() {
        UserComment userComment = new UserComment();
        BeanUtils.copyProperties(this, userComment);
        userComment.setUserId(UserContext.getUserId());
        userComment.setTimestamp(LocalDateTime.now());
        userComment.setStatus(UserComment.Status.ENABLE);
        userComment.setContent(SensitiveUtils.sensitiveFilter(content));
        return userComment;
    }
}
