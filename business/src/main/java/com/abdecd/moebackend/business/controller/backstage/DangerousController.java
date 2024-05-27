package com.abdecd.moebackend.business.controller.backstage;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.dao.entity.BangumiTimeTable;
import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.entity.VideoSrc;
import com.abdecd.moebackend.common.result.Result;
import com.abdecd.tokenlogin.aspect.RequirePermission;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "危险接口")
@RestController
@RequestMapping("/backstage/dangerous")
public class DangerousController {
    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "添加视频")
    @PostMapping("video/add")
    public Result<String> addVideo(@RequestBody List<Video> video) {
        Db.saveBatch(video);
        return Result.success();
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "删除视频")
    @PostMapping("video/delete")
    public Result<String> deleteVideo(@RequestBody List<Video> video) {
        Db.removeByIds(video.stream().map(Video::getId).toList(), Video.class);
        return Result.success();
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "更新视频")
    @PostMapping("video/update")
    public Result<String> updateVideo(@RequestBody List<Video> video) {
        Db.updateBatchById(video);
        return Result.success();
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "更新视频源")
    @PostMapping("video-src/update")
    public Result<String> updateVideoSrc(@RequestBody List<VideoSrc> src) {
        Db.updateBatchById(src);
        return Result.success();
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "添加视频源")
    @PostMapping("video-src/add")
    public Result<String> addVideoSrc(@RequestBody List<VideoSrc> src) {
        Db.saveBatch(src);
        return Result.success();
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "删除视频源")
    @PostMapping("video-src/delete")
    public Result<String> deleteVideoSrc(@RequestBody List<VideoSrc> src) {
        Db.removeByIds(src.stream().map(VideoSrc::getId).toList(), VideoSrc.class);
        return Result.success();
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "更新新番时间表")
    @PostMapping("bangumi/update")
    public Result<String> updateBangumi(@RequestBody List<BangumiTimeTable> bangumiTimeTables) {
        Db.updateBatchById(bangumiTimeTables);
        return Result.success();
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "添加新番时间表")
    @PostMapping("bangumi/add")
    public Result<String> addBangumi(@RequestBody List<BangumiTimeTable> bangumiTimeTables) {
        Db.saveBatch(bangumiTimeTables);
        return Result.success();
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "删除新番时间表")
    @PostMapping("bangumi/delete")
    public Result<String> deleteBangumi(@RequestBody List<BangumiTimeTable> bangumiTimeTables) {
        Db.removeByIds(bangumiTimeTables.stream().map(BangumiTimeTable::getId).toList(), BangumiTimeTable.class);
        return Result.success();
    }
}
