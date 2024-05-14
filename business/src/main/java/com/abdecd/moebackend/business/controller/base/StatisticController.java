package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.lib.RateLimiter;
import com.abdecd.moebackend.business.pojo.dto.statistic.VideoPlayDTO;
import com.abdecd.moebackend.business.pojo.vo.statistic.StatisticDataVO;
import com.abdecd.moebackend.business.service.statistic.StatisticService;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.result.Result;
import com.abdecd.tokenlogin.common.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Tag(name = "统计")
@RestController
@RequestMapping("/statistic")
public class StatisticController {
    @Autowired
    private RateLimiter rateLimiter;
    @Autowired
    private StatisticService statisticService;

    @Operation(summary = "播放行为统计")
    @PostMapping("video-play")
    public Result<String> videoPlay(@RequestBody @Valid VideoPlayDTO videoPlayDTO) {
        if (rateLimiter.isRateLimited(
                RedisConstant.STATISTIC_VIDEO_PLAY_LOCK + UserContext.getUserId(),
                1,
                RedisConstant.STATISTIC_VIDEO_PLAY_RESET_TIME,
                TimeUnit.SECONDS
        )) return Result.error(MessageConstant.RATE_LIMIT);
        statisticService.cntVideoPlay(videoPlayDTO, RedisConstant.STATISTIC_VIDEO_PLAY_RESET_TIME);
        return Result.success();
    }

    @Operation(summary = "获取视频观看量等统计数据")
    @GetMapping("video-group-data")
    public Result<StatisticDataVO> getVideoGroupData(@RequestParam Long id) {
        return Result.success(statisticService.getStatisticData(id));
    }
}
