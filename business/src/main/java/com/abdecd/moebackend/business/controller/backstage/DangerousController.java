package com.abdecd.moebackend.business.controller.backstage;

import com.abdecd.moebackend.business.dao.entity.BangumiTimeTable;
import com.abdecd.moebackend.business.dao.entity.Danmaku;
import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.entity.VideoSrc;
import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.service.search.SearchService;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.business.tokenLogin.aspect.RequirePermission;
import com.abdecd.moebackend.common.result.Result;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RequirePermission(value = "99", exception = BaseException.class)
@Tag(name = "危险接口")
@RestController
@RequestMapping("/backstage/dangerous")
public class DangerousController {
    @Autowired
    private SearchService searchService;
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;

    @Operation(summary = "添加视频")
    @PostMapping("video/add")
    public Result<List<Long>> addVideo(@RequestBody List<Video> video) {
        Db.saveBatch(video);
        return Result.success(video.stream().map(Video::getId).toList());
    }

    @Operation(summary = "删除视频")
    @PostMapping("video/delete")
    public Result<String> deleteVideo(@RequestBody List<Video> video) {
        Db.removeByIds(video.stream().map(Video::getId).toList(), Video.class);
        return Result.success();
    }

    @Operation(summary = "更新视频")
    @PostMapping("video/update")
    public Result<String> updateVideo(@RequestBody List<Video> video) {
        Db.updateBatchById(video);
        return Result.success();
    }

    @Operation(summary = "更新视频源")
    @PostMapping("video-src/update")
    public Result<String> updateVideoSrc(@RequestBody List<VideoSrc> src) {
        Db.updateBatchById(src);
        return Result.success();
    }

    @Operation(summary = "添加视频源")
    @PostMapping("video-src/add")
    public Result<List<Long>> addVideoSrc(@RequestBody List<VideoSrc> src) {
        Db.saveBatch(src);
        return Result.success(src.stream().map(VideoSrc::getId).toList());
    }

    @Operation(summary = "删除视频源")
    @PostMapping("video-src/delete")
    public Result<String> deleteVideoSrc(@RequestBody List<VideoSrc> src) {
        Db.removeByIds(src.stream().map(VideoSrc::getId).toList(), VideoSrc.class);
        return Result.success();
    }

    @Operation(summary = "更新新番时间表")
    @PostMapping("bangumi/update")
    public Result<String> updateBangumi(@RequestBody List<BangumiTimeTable> bangumiTimeTables) {
        Db.updateBatchById(bangumiTimeTables);
        return Result.success();
    }

    @Operation(summary = "添加新番时间表")
    @PostMapping("bangumi/add")
    public Result<List<Long>> addBangumi(@RequestBody List<BangumiTimeTable> bangumiTimeTables) {
        Db.saveBatch(bangumiTimeTables);
        return Result.success(bangumiTimeTables.stream().map(BangumiTimeTable::getId).toList());
    }

    @Operation(summary = "删除新番时间表")
    @PostMapping("bangumi/delete")
    public Result<String> deleteBangumi(@RequestBody List<BangumiTimeTable> bangumiTimeTables) {
        Db.removeByIds(bangumiTimeTables.stream().map(BangumiTimeTable::getId).toList(), BangumiTimeTable.class);
        return Result.success();
    }

    @Operation(summary = "更新视频组")
    @PostMapping("video-group/update")
    public Result<String> updateVideoGroup(@RequestBody List<com.abdecd.moebackend.business.dao.entity.VideoGroup> videoGroups) {
        Db.updateBatchById(videoGroups);
        return Result.success();
    }

    @Operation(summary = "添加视频组")
    @PostMapping("video-group/add")
    public Result<List<Long>> addVideoGroup(@RequestBody List<com.abdecd.moebackend.business.dao.entity.VideoGroup> videoGroups) {
        Db.saveBatch(videoGroups);
        try {
            searchService.initData(videoGroups.stream().map(x -> videoGroupServiceBase.getVideoGroupInfo(x.getId())).collect(Collectors.toList()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Result.success(videoGroups.stream().map(com.abdecd.moebackend.business.dao.entity.VideoGroup::getId).toList());
    }

    @Operation(summary = "删除视频组")
    @PostMapping("video-group/delete")
    public Result<String> deleteVideoGroup(@RequestBody List<com.abdecd.moebackend.business.dao.entity.VideoGroup> videoGroups) {
        Db.removeByIds(videoGroups.stream().map(com.abdecd.moebackend.business.dao.entity.VideoGroup::getId).toList(), com.abdecd.moebackend.business.dao.entity.VideoGroup.class);
        return Result.success();
    }

    @Operation(summary = "更新番剧视频组")
    @PostMapping("bangumi-video-group/update")
    public Result<String> updateBangumiVideoGroup(@RequestBody List<com.abdecd.moebackend.business.dao.entity.BangumiVideoGroup> bangumiVideoGroups) {
        Db.updateBatchById(bangumiVideoGroups);
        return Result.success();
    }

    @Operation(summary = "添加番剧视频组")
    @PostMapping("bangumi-video-group/add")
    public Result<List<Long>> addBangumiVideoGroup(@RequestBody List<com.abdecd.moebackend.business.dao.entity.BangumiVideoGroup> bangumiVideoGroups) {
        Db.saveBatch(bangumiVideoGroups);
        return Result.success(bangumiVideoGroups.stream().map(com.abdecd.moebackend.business.dao.entity.BangumiVideoGroup::getVideoGroupId).toList());
    }

    @Operation(summary = "删除番剧视频组")
    @PostMapping("bangumi-video-group/delete")
    public Result<String> deleteBangumiVideoGroup(@RequestBody List<com.abdecd.moebackend.business.dao.entity.BangumiVideoGroup> bangumiVideoGroups) {
        Db.removeByIds(bangumiVideoGroups.stream().map(com.abdecd.moebackend.business.dao.entity.BangumiVideoGroup::getVideoGroupId).toList(), com.abdecd.moebackend.business.dao.entity.BangumiVideoGroup.class);
        return Result.success();
    }

    @Operation(summary = "导入弹幕")
    @PostMapping("danmaku/import")
    public Result<List<Long>> importDanmaku(@RequestBody List<Danmaku> list) {
        Db.saveBatch(list);
        return Result.success(list.stream().map(Danmaku::getId).toList());
    }
}
