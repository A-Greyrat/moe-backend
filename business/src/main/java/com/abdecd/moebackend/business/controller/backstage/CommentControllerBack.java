package com.abdecd.moebackend.business.controller.backstage;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.pojo.dto.comment.DeleteCommentDTO;
import com.abdecd.moebackend.business.pojo.vo.comment.UserCommentVO;
import com.abdecd.moebackend.business.service.comment.CommentService;
import com.abdecd.moebackend.common.result.PageVO;
import com.abdecd.moebackend.common.result.Result;
import com.abdecd.tokenlogin.aspect.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequirePermission(value = "99", exception = BaseException.class)
@Tag(name = "评论接口")
@RestController
@RequestMapping("/backstage/video/comment")
public class CommentControllerBack {
    @Autowired
    private CommentService commentService;

    @Operation(summary = "获取评论")
    @GetMapping("")
    public Result<PageVO<List<UserCommentVO>>> getComment(
            @NotNull @Schema(description = "视频id") Long videoId,
            @NotNull @Schema(description = "页码") @Min(1) Integer page,
            @NotNull @Schema(description = "每页数量") @Min(0) Integer pageSize
    ) {
        return Result.success(commentService.getComment(videoId, page, pageSize));
    }

    @Operation(summary = "删除评论")
    @PostMapping("delete")
    public Result<String> deleteComment(@RequestBody @Valid DeleteCommentDTO deleteCommentDTO) {
        commentService.forceDeleteComment(deleteCommentDTO.getId());
        return Result.success();
    }
}
