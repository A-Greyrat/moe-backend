package com.abdecd.moebackend.business.controller.base.videogroup;

import com.abdecd.moebackend.business.common.util.HttpCacheUtils;
import com.abdecd.moebackend.business.pojo.vo.videogroup.BangumiVideoGroupTimeScheduleVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.BangumiVideoGroupVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.ContentsItemVO;
import com.abdecd.moebackend.business.service.videogroup.BangumiVideoGroupServiceBase;
import com.abdecd.moebackend.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "番剧视频组接口")
@RestController
@RequestMapping("bangumi-video-group")
public class BangumiVideoGroupControllerBase {
    @Autowired
    private BangumiVideoGroupServiceBase bangumiVideoGroupServiceBase;

    @Operation(summary = "获取视频组信息")
    @GetMapping("")
    public Result<BangumiVideoGroupVO> getVideoGroup(@RequestParam Long id) {
        return Result.success(bangumiVideoGroupServiceBase.getVideoGroupInfo(id));
    }

    @Operation(summary = "获取视频组目录")
    @GetMapping("contents")
    public Result<List<ContentsItemVO>> getContents(@RequestParam Long id) {
        return Result.success(bangumiVideoGroupServiceBase.getContents(id));
    }

    @Operation(summary = "获取新番时间表")
    @GetMapping("time-schedule")
    public Result<List<BangumiVideoGroupTimeScheduleVO>> getTimeSchedule(
            @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var vo = bangumiVideoGroupServiceBase.getTimeSchedule(date);
        if (HttpCacheUtils.tryUseCache(request, response, vo)) return null;
        return Result.success(vo);
    }
}
