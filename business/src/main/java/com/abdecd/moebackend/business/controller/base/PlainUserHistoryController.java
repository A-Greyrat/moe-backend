package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.pojo.dto.plainuser.DeleteHistoryDTO;
import com.abdecd.moebackend.business.pojo.vo.plainuser.HistoryVO;
import com.abdecd.moebackend.business.service.plainuser.PlainUserHistoryService;
import com.abdecd.moebackend.business.service.statistic.LastWatchTimeStatistic;
import com.abdecd.moebackend.common.result.PageVO;
import com.abdecd.moebackend.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户观看历史接口")
@RestController
@RequestMapping("/plain-user/history")
public class PlainUserHistoryController {
    @Autowired
    private PlainUserHistoryService plainUserHistoryService;
    @Autowired
    private LastWatchTimeStatistic lastWatchTimeStatistic;

//    @Operation(summary = "获取用户观看历史")
//    @GetMapping("")
//    public Result<PageVO<HistoryVO>> getHistory(
//            @RequestParam(defaultValue = "1") Integer page,
//            @RequestParam(defaultValue = "10") Integer pageSize
//    ) {
//        return Result.success(plainUserHistoryService.getHistory(page, pageSize));
//    }

    @Operation(summary = "获取用户观看历史")
    @GetMapping("")
    public Result<PageVO<HistoryVO>> getHistory(
            @RequestParam(defaultValue = "0") @Min(0) Integer index,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) Integer pageSize
    ) {
        return Result.success(plainUserHistoryService.getHistory2(index, pageSize));
    }

    @Operation(summary = "删除用户观看历史")
    @PostMapping("delete")
    public Result<String> deleteHistory(@RequestBody @Valid DeleteHistoryDTO deleteHistoryDTO) {
        plainUserHistoryService.deleteHistory(deleteHistoryDTO.getVideoGroupIds());
        return Result.success();
    }

    @Operation(summary = "获取用户视频组上次看到哪集")
    @GetMapping("video-group")
    public Result<HistoryVO> getHistory(@RequestParam long videoGroupId) {
        return Result.success(plainUserHistoryService.getLatestHistory(videoGroupId));
    }

    @Operation(summary = "获取用户视频上次看到哪里")
    @GetMapping("video-last-watch-time")
    public Result<Long> addHistory(@RequestParam Long videoId) {
        return Result.success(lastWatchTimeStatistic.get(videoId));
    }
}
