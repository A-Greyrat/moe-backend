package com.abdecd.moebackend.business.controller.backstage;

import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.pojo.dto.backstage.commonVideoGroup.VideoGroupAddDTO;
import com.abdecd.moebackend.business.pojo.dto.backstage.commonVideoGroup.VideoGroupDTO;
import com.abdecd.moebackend.business.pojo.dto.backstage.videogroup.VideoGroupDeleteDTO;
import com.abdecd.moebackend.business.pojo.vo.backstage.commonVideoGroup.VideoGroupVO;
import com.abdecd.moebackend.business.pojo.vo.backstage.commonVideoGroup.VideoVo;
import com.abdecd.moebackend.business.pojo.vo.backstage.videoGroup.PlainVideoGroupVO;
import com.abdecd.moebackend.business.pojo.vo.statistic.StatisticDataVO;
import com.abdecd.moebackend.business.service.backstage.PlainVideoGroupService;
import com.abdecd.moebackend.business.service.backstage.VideoGroupService;
import com.abdecd.moebackend.business.service.statistic.StatisticService;
import com.abdecd.moebackend.business.tokenLogin.aspect.RequirePermission;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.result.PageVO;
import com.abdecd.moebackend.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@RequirePermission(value = "99", exception = BaseException.class)
@Tag(name = "后台普通视频组接口")
@Slf4j
@RestController
@RequestMapping("/backstage/plain-video-group")
public class PlainVideoGroupController {

    @Resource
    private VideoGroupService videoGroupService;

    @Resource
    private StatisticService statisticService;

    @Resource
    private PlainVideoGroupService plainVideoGroupService;

    @Operation(summary = "普通视频组添加", description = "data字段返回新增普通视频组id")
    @PostMapping(value = "/add", consumes = "multipart/form-data")
    @ResponseBody
    public Result<Long> addVideoGroup(@Valid VideoGroupAddDTO videoGroupAddDTO) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
        LocalDateTime ldt = LocalDateTime.now();
        ldt.format(dtf);

        Long groupId = videoGroupService.insert(new VideoGroup().setCreateTime(ldt).setTitle(videoGroupAddDTO.getTitle()).setDescription(videoGroupAddDTO.getDescription()).setTags(videoGroupAddDTO.getTags()).setVideoGroupStatus(VideoGroup.Status.TRANSFORMING), videoGroupAddDTO.getCover());

//        if (videoGroupAddDTO.getTags() != null) {
//            String[] tags = videoGroupAddDTO.getTags().split(";");
//            for (String i : tags) {
//                Long tagId = Long.valueOf(i);
//                videoGroupAndTagService.insert(tagId, groupId);
//            }
//        }

        return Result.success(groupId);
    }

    @Operation(summary = "普通视频组删除")
    @PostMapping(value = "/delete")
    @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CACHE, beforeInvocation = true, key = "#videoDeleteDTO.id")
    public Result<String> delVideoGroup(@RequestBody @Valid VideoGroupDeleteDTO videoDeleteDTO) {
//        videoGroupService.delete(id);
//        videoGroupAndTagService.deleteByVideoGroupId(id);
        videoGroupService.deleteVideoGroup(videoDeleteDTO.getId());
        return Result.success();
    }

    @Operation(summary = "普通视频组更新")
    @PostMapping(value = "/update", consumes = "multipart/form-data")
    @ResponseBody
    @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CACHE, beforeInvocation = true, key = "#videoGroupDTO.id")
    public Result<String> updateVideoGroup(@Valid VideoGroupDTO videoGroupDTO) {
        videoGroupService.update(videoGroupDTO);
//        videoGroupAndTagService.update(videoGroupDTO);
        return Result.success();
    }

    @Operation(summary = "普通视频组获取", description = "data字段返回普通视频组信息")
    @GetMapping("")
    public Result<VideoGroupVO> getVideoGroup(@Valid @RequestParam("id") Long id) {
        VideoGroupVO videoGroupVO = videoGroupService.getById(id);

        StatisticDataVO statisticDataVO = statisticService.getFullStatisticData(id);
        videoGroupVO.setWatchCnt(Math.toIntExact(statisticDataVO.getWatchCnt()));
        videoGroupVO.setFavoriteCnt(Math.toIntExact(statisticDataVO.getFavoriteCnt()));
        videoGroupVO.setLikeCnt(Math.toIntExact(statisticDataVO.getLikeCnt()));
        videoGroupVO.setUserLike(statisticDataVO.getUserLike());
        videoGroupVO.setUserFavorite(statisticDataVO.getUserFavorite());
        videoGroupVO.setCommentCnt(Math.toIntExact(statisticDataVO.getCommentCnt()));
        videoGroupVO.setDanmakuCnt(Math.toIntExact(statisticDataVO.getDanmakuCnt()));

        return Result.success(videoGroupVO);
    }

    @Operation(summary = "普通视频组目录获取", description = "data字段返回普通视频组目录")
    @GetMapping("/contents")
    public Result<ArrayList<VideoVo>> getVideoGroupContent(@Valid @RequestParam("id") Long id) {
        ArrayList<VideoVo> videoVoList = videoGroupService.getContentById(id);
        return Result.success(videoVoList);
    }


    @Operation(summary = "获取所有符合条件的普通视频组", description = "data字段返回普通视频组信息")
    @GetMapping("/all")
    public Result<PageVO<PlainVideoGroupVO>> getAllVideoGroup(@RequestParam(name = "page", defaultValue = "1", required = false) @Valid Integer pageIndex, @RequestParam(defaultValue = "10", required = false) @Valid Integer pageSize, @RequestParam(required = false) @Valid String id, @RequestParam(required = false) @Valid String title, @RequestParam(required = false) @Valid Byte status) {

        var videoGroupVOList = plainVideoGroupService.getAllVideoGroup((pageIndex - 1) * pageSize, pageSize, id, title, status);
        var total = plainVideoGroupService.countPlainVideoGroup(id, title, status);
        return Result.success(new PageVO<>(total, videoGroupVOList));
    }
}
