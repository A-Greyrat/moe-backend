package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.common.util.HttpCacheUtils;
import com.abdecd.moebackend.business.lib.RateLimiter;
import com.abdecd.moebackend.business.pojo.dto.danmaku.AddDanmakuDTO;
import com.abdecd.moebackend.business.pojo.vo.danmaku.DanmakuVO;
import com.abdecd.moebackend.business.service.danmaku.DanmakuService;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.result.Result;
import com.abdecd.tokenlogin.common.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "弹幕接口")
@RestController
@RequestMapping("/video/danmaku")
public class DanmakuController {
    @Autowired
    private DanmakuService danmakuService;
    @Autowired
    private RateLimiter rateLimiter;

    @Operation(summary = "获取弹幕")
    @GetMapping("")
    public Result<List<DanmakuVO>> getDanmaku(
            @RequestParam Long videoId,
            @RequestParam(defaultValue = "1") Integer segmentIndex,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (HttpCacheUtils.tryUseCache(request, response, danmakuService.getDanmakuTimestamp(videoId, segmentIndex))) return null;
        return Result.success(danmakuService.getDanmaku(videoId, segmentIndex));
    }

    @Operation(summary = "添加弹幕")
    @PostMapping("add")
    public Result<Long> addDanmaku(@RequestBody @Valid AddDanmakuDTO addDanmakuDTO) {
        if (rateLimiter.isRateLimited(
            RedisConstant.LIMIT_DANMAKU_USER_MODIFY + UserContext.getUserId(),
            1,
            RedisConstant.LIMIT_DANMAKU_USER_MODIFY_RESET_TIME,
            TimeUnit.SECONDS)
        ) throw new BaseException(MessageConstant.RATE_LIMIT);
        return Result.success(danmakuService.addDanmaku(addDanmakuDTO));
    }

    @Operation(summary = "撤回弹幕")
    @PostMapping("delete")
    public Result<String> deleteDanmaku(@RequestBody Map<String, Long> map) {
        if (rateLimiter.isRateLimited(
            RedisConstant.LIMIT_DANMAKU_USER_MODIFY + UserContext.getUserId(),
            1,
            RedisConstant.LIMIT_DANMAKU_USER_MODIFY_RESET_TIME,
            TimeUnit.SECONDS)
        ) throw new BaseException(MessageConstant.RATE_LIMIT);
        danmakuService.deleteDanmaku(map.get("id"));
        return Result.success();
    }
}
