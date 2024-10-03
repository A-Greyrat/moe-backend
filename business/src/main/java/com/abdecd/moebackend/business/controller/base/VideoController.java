package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.common.util.HttpCacheUtils;
import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.pojo.dto.plainuser.AddHistoryDTO;
import com.abdecd.moebackend.business.pojo.vo.video.VideoVO;
import com.abdecd.moebackend.business.service.plainuser.PlainUserHistoryService;
import com.abdecd.moebackend.business.service.video.VideoService;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.*;

@Tag(name = "视频接口")
@RestController
@RequestMapping("video")
public class VideoController {
    @Autowired
    private VideoService videoService;
    @Autowired
    private PlainUserHistoryService plainUserHistoryService;
    private final ExecutorService executor = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100000));

//    @Operation(summary = "添加视频")
//    @PostMapping("add")
//    public Result<Long> addVideo(@RequestBody @Valid AddVideoDTO addVideoDTO) {
//        return Result.success(videoService.addVideo(addVideoDTO));
//    }

    @Operation(summary = "添加或修改的视频是否正在处理")
    @GetMapping("check-video-pending")
    public Result<Boolean> checkAddVideoPending(@RequestParam Long videoId) {
        return Result.success(videoService.checkVideoPending(videoId));
    }

//    @Operation(summary = "修改视频")
//    @PostMapping("update")
//    public Result<String> updateVideo(@RequestBody @Valid UpdateVideoDTO updateVideoDTO) {
//        videoService.updateVideo(updateVideoDTO);
//        return Result.success();
//    }

//    @Operation(summary = "删除视频")
//    @PostMapping("delete")
//    public Result<String> deleteVideo(@RequestBody @Valid DeleteVideoDTO deleteVideoDTO) {
//        videoService.deleteVideo(deleteVideoDTO.getId());
//        return Result.success();
//    }

    @Async
    @Operation(summary = "获取视频")
    @GetMapping("")
    public CompletableFuture<Result<VideoVO>> getVideo(
            @RequestParam Long id,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var video = videoService.getVideo(id);
        if (video == null) throw new BaseException(MessageConstant.VIDEO_NOT_FOUND);
        // 添加观看历史记录
        var userId = UserContext.getUserId();
        if (userId != null) executor.submit(() -> plainUserHistoryService.addHistory(new AddHistoryDTO(userId, id)));

        if (HttpCacheUtils.tryUseCache(request, response, video)) return null;
        return CompletableFuture.completedFuture(Result.success(video));
    }
}