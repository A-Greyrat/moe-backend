package com.abdecd.moebackend.business.controller.base.videogroup;

import com.abdecd.moebackend.business.common.util.HttpCacheUtils;
import com.abdecd.moebackend.business.pojo.dto.videogroup.LikeDTO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupBigVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupWithDataVO;
import com.abdecd.moebackend.business.service.FavoriteService;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.common.result.PageVO;
import com.abdecd.moebackend.common.result.Result;
import com.abdecd.tokenlogin.common.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Tag(name = "视频组接口")
@RestController
@RequestMapping("/video-group")
public class VideoGroupControllerBase {
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;
    @Autowired
    private FavoriteService favoriteService;

    @Operation(summary = "获取视频组类型")
    @GetMapping("type")
    public Result<Byte> getVideoGroupType(@RequestParam Long id) {
        return Result.success(videoGroupServiceBase.getVideoGroupType(id));
    }

    @Async
    @Operation(summary = "获取视频组大接口")
    @GetMapping("")
    public CompletableFuture<Result<VideoGroupBigVO>> getVideoGroup(
            @RequestParam Long id,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var vo = videoGroupServiceBase.getBigVideoGroup(id);
        if (HttpCacheUtils.tryUseCache(request, response, vo)) return null;
        return CompletableFuture.completedFuture(Result.success(vo));
    }

    @Operation(summary = "视频组点赞/取消点赞")
    @PostMapping("like")
    public Result<String> like(@RequestBody @Valid LikeDTO likeDTO) {
        favoriteService.addOrDeleteLike(UserContext.getUserId(), likeDTO.getId(), likeDTO.getStatus());
        return Result.success();
    }

    @Operation(summary = "获取投稿列表")
    @GetMapping("my-upload-list")
    public Result<PageVO<VideoGroupWithDataVO>> like(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(videoGroupServiceBase.pageMyUploadVideoGroup(page, pageSize));
    }
}