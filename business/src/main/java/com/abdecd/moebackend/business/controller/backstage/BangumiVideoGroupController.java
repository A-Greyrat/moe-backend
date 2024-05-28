package com.abdecd.moebackend.business.controller.backstage;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.dao.entity.BangumiVideoGroup;
import com.abdecd.moebackend.business.dao.mapper.BangumiTimeTableMapper;
import com.abdecd.moebackend.business.pojo.dto.backstage.bangumiVideoGroup.BangumiVideoGroupAddDTO;
import com.abdecd.moebackend.business.pojo.dto.backstage.bangumiVideoGroup.BangumiVideoGroupUpdateDTO;
import com.abdecd.moebackend.business.pojo.dto.backstage.videogroup.VideoGroupDeleteDTO;
import com.abdecd.moebackend.business.pojo.vo.backstage.bangumiVideoGroup.BangumiVideoGroupVO;
import com.abdecd.moebackend.business.pojo.vo.backstage.commonVideoGroup.VideoVo;
import com.abdecd.moebackend.business.pojo.vo.statistic.StatisticDataVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.BangumiTimeTableBackVO;
import com.abdecd.moebackend.business.service.backstage.BangumiVideoGroupService;
import com.abdecd.moebackend.business.service.backstage.VideoGroupService;
import com.abdecd.moebackend.business.service.statistic.StatisticService;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.result.PageVO;
import com.abdecd.moebackend.common.result.Result;
import com.abdecd.tokenlogin.aspect.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Tag(name = "后台番剧视频组接口")
@Slf4j
@RestController
@RequestMapping("/backstage/bangumi-video-group")
public class BangumiVideoGroupController {
    @Resource
    private VideoGroupService videoGroupService;
    @Resource
    private BangumiVideoGroupService bangumiVideoGroupService;
    @Resource
    private StatisticService statisticService;
    @Autowired
    private BangumiTimeTableMapper bangumiTimeTableMapper;

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "番剧视频组添加", description = "data字段返回新增视频组id")
    @PostMapping(value = "/add", consumes = "multipart/form-data")
    @ResponseBody
    public Result<Long> addBangumiVideoGroup(@Valid BangumiVideoGroupAddDTO bangumiVideoGroupAddDTO) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        now.format(formatter);

        Long vid = bangumiVideoGroupService.insert(bangumiVideoGroupAddDTO);

        BangumiVideoGroup bangumiVideoGroup = new BangumiVideoGroup();
        bangumiVideoGroup.setVideoGroupId(vid);
        bangumiVideoGroup.setUpdateAtAnnouncement(bangumiVideoGroupAddDTO.getUpdateAtAnnouncement());
        bangumiVideoGroup.setReleaseTime(bangumiVideoGroupAddDTO.getReleaseTime());
        bangumiVideoGroup.setStatus(Integer.valueOf(bangumiVideoGroupAddDTO.getStatus()));
        bangumiVideoGroup.setUpdateTime(now);

        bangumiVideoGroupService.insert(bangumiVideoGroup);

        return Result.success(vid);
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "番剧视频组删除")
    @PostMapping(value = "/delete")
    @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CACHE, beforeInvocation = true, key = "#videoDeleteDTO.id")
    public Result<String> deleteBangumiVideoGroup(@RequestBody @Valid VideoGroupDeleteDTO videoDeleteDTO) {
        Long id = videoDeleteDTO.getId();
//        videoGroupService.delete(id);
        bangumiVideoGroupService.deleteByVid(id);
//        videoGroupAndTagService.deleteByVideoGroupId(id);
        videoGroupService.deleteVideoGroup(id);
        return Result.success();
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "番剧视频组更新")
    @PostMapping(value = "/update", consumes = "multipart/form-data")
    @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CACHE, beforeInvocation = true, key = "#bangumiVideoGroupUpdateDTO.id")
    public Result<String> updateBangumiVideoGroup(@Valid BangumiVideoGroupUpdateDTO bangumiVideoGroupUpdateDTO) {
        bangumiVideoGroupService.update(bangumiVideoGroupUpdateDTO);
        videoGroupService.update(bangumiVideoGroupUpdateDTO);
//        videoGroupAndTagService.update(bangumiVideoGroupUpdateDTO);
        return Result.success();
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "番剧视频组获取", description = "data字段返回番剧视频组信息")
    @GetMapping("")
    public Result<BangumiVideoGroupVO> getBangumiVideoGroupInfo(@Valid @RequestParam("id") Long id) {
        BangumiVideoGroupVO bangumiVideoGroupVO = new BangumiVideoGroupVO();
        bangumiVideoGroupVO.setId(String.valueOf(id));

        bangumiVideoGroupVO = bangumiVideoGroupService.getByVideoId(Long.valueOf(bangumiVideoGroupVO.getId()));
        BangumiVideoGroupVO bangumiVideoGroupVO_ = bangumiVideoGroupService.getByVid(Long.valueOf(bangumiVideoGroupVO.getId()));

        bangumiVideoGroupVO.setReleaseTime(bangumiVideoGroupVO_.getReleaseTime());
        bangumiVideoGroupVO.setUpdateAtAnnouncement(bangumiVideoGroupVO_.getUpdateAtAnnouncement());
        bangumiVideoGroupVO.setStatus(bangumiVideoGroupVO_.getStatus());

        StatisticDataVO statisticDataVO = statisticService.getFullStatisticData(id);
        bangumiVideoGroupVO.setWatchCnt(Math.toIntExact(statisticDataVO.getWatchCnt()));
        bangumiVideoGroupVO.setFavoriteCnt(Math.toIntExact(statisticDataVO.getFavoriteCnt()));
        bangumiVideoGroupVO.setLikeCnt(Math.toIntExact(statisticDataVO.getLikeCnt()));
        bangumiVideoGroupVO.setUserLike(statisticDataVO.getUserLike());
        bangumiVideoGroupVO.setUserFavorite(statisticDataVO.getUserFavorite());
        bangumiVideoGroupVO.setCommentCnt(Math.toIntExact(statisticDataVO.getCommentCnt()));
        bangumiVideoGroupVO.setDanmakuCnt(Math.toIntExact(statisticDataVO.getDanmakuCnt()));

        return Result.success(bangumiVideoGroupVO);
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "番剧视频组目录获取", description = "data字段返回番剧视频组目录")
    @GetMapping("/contents")
    public Result<ArrayList<VideoVo>> getBangumiVideoGroupContent(@Valid @RequestParam("id") Long id) {
        ArrayList<VideoVo> videoVoList = videoGroupService.getContentById(id);
        return Result.success(videoVoList);
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "获取所有符合条件的番剧视频组", description = "data字段返回番剧视频组信息")
    @GetMapping("/all")
    public Result<PageVO<com.abdecd.moebackend.business.pojo.vo.backstage.videoGroup.BangumiVideoGroupVO>> getAllBangumiVideoGroup(
            @RequestParam(name="page", defaultValue = "1", required = false) @Valid Integer pageIndex,
            @Valid @RequestParam(defaultValue = "10", required = false) Integer pageSize,
            @Valid @RequestParam(required = false) String id,
            @Valid @RequestParam(required = false) String title,
            @Valid @RequestParam(required = false) Byte status) {

        var list = bangumiVideoGroupService.getBangumiVideoGroupList((pageIndex - 1) * pageSize, pageSize, id, title, status);
        var total = bangumiVideoGroupService.getBangumiVideoGroupListCount(id, title, status);
        return Result.success(new PageVO<com.abdecd.moebackend.business.pojo.vo.backstage.videoGroup.BangumiVideoGroupVO>().setRecords(list).setTotal(total));
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "获取新番时间表")
    @GetMapping("time-schedule")
    public Result<PageVO<BangumiTimeTableBackVO>> getBangumiVideoGroupList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        var pageObj = new Page<BangumiTimeTableBackVO>(page, pageSize);
        var result = bangumiTimeTableMapper.pageBangumiTimeTable(pageObj);
        var vo = new PageVO<>((int) result.getTotal(), result.getRecords());
        return Result.success(vo);
    }
}
