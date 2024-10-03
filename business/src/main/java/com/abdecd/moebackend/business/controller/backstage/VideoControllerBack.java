package com.abdecd.moebackend.business.controller.backstage;

import com.abdecd.moebackend.business.dao.entity.BangumiTimeTable;
import com.abdecd.moebackend.business.dao.entity.BangumiVideoGroup;
import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.BangumiTimeTableMapper;
import com.abdecd.moebackend.business.dao.mapper.BangumiVideoGroupMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoMapper;
import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.pojo.dto.video.AddVideoFullDTO;
import com.abdecd.moebackend.business.pojo.dto.video.DeleteVideoDTO;
import com.abdecd.moebackend.business.pojo.dto.video.UpdateManyVideoIndexDTO;
import com.abdecd.moebackend.business.pojo.dto.video.UpdateVideoFullDTO;
import com.abdecd.moebackend.business.pojo.vo.video.VideoForceWithWillUpdateTimeVO;
import com.abdecd.moebackend.business.service.BangumiTimeTableService;
import com.abdecd.moebackend.business.service.video.VideoService;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.business.tokenLogin.aspect.RequirePermission;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@RequirePermission(value = "99", exception = BaseException.class)
@Tag(name = "视频接口")
@RestController
@RequestMapping("/backstage/video")
public class VideoControllerBack {
    @Resource
    private VideoService videoService;
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;
    @Autowired
    private BangumiTimeTableMapper bangumiTimeTableMapper;
    @Autowired
    private BangumiVideoGroupMapper bangumiVideoGroupMapper;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private BangumiTimeTableService bangumiTimeTableService;

    @Operation(summary = "添加视频")
    @PostMapping("add")
    public Result<Long> add(@RequestBody @Valid AddVideoFullDTO addVideoDTO) {
        if (addVideoDTO.getCover() == null) {
            // 默认封面
            var vg = videoGroupServiceBase.getVideoGroupInfoForce(addVideoDTO.getVideoGroupId());
            if (vg == null) throw new BaseException(MessageConstant.INVALID_VIDEO_GROUP);
            addVideoDTO.setCover(vg.getCover());
        }
        long id = videoService.addVideo(addVideoDTO, addVideoDTO.getVideoStatusWillBe(), false);
        // 更新修改时间
        if (Objects.equals(videoGroupServiceBase.getVideoGroupType(addVideoDTO.getVideoGroupId()), VideoGroup.Type.ANIME_VIDEO_GROUP)) {
            if (Objects.equals(addVideoDTO.getVideoStatusWillBe(), Video.Status.ENABLE)) {
                bangumiVideoGroupMapper.update(new LambdaUpdateWrapper<BangumiVideoGroup>()
                    .eq(BangumiVideoGroup::getVideoGroupId, addVideoDTO.getVideoGroupId())
                    .set(BangumiVideoGroup::getUpdateTime, LocalDateTime.now())
                );
            }
        }
        bangumiTimeTableService.addVideoCb(addVideoDTO, id);
        return Result.success(id);
    }

    @Operation(summary = "修改视频")
    @PostMapping("update")
    public Result<String> update(@RequestBody @Valid UpdateVideoFullDTO dto) {
        videoService.updateVideo(dto, dto.getVideoStatusWillBe());
        var video = videoService.getVideoForce(dto.getId());
        // 更新修改时间
        if (Objects.equals(videoGroupServiceBase.getVideoGroupType(dto.getVideoGroupId()), VideoGroup.Type.ANIME_VIDEO_GROUP)) {
            bangumiVideoGroupMapper.update(new LambdaUpdateWrapper<BangumiVideoGroup>()
                .eq(BangumiVideoGroup::getVideoGroupId, video.getVideoGroupId())
                .set(BangumiVideoGroup::getUpdateTime, LocalDateTime.now())
            );
        }
        bangumiTimeTableService.updateVideoCb(dto, video);
        return Result.success();
    }

    @Operation(summary = "删除视频")
    @PostMapping("delete")
    public Result<String> delete(@RequestBody @Valid DeleteVideoDTO dto) {
        videoService.deleteVideo(dto.getId());
        bangumiTimeTableMapper.delete(new LambdaQueryWrapper<BangumiTimeTable>()
            .eq(BangumiTimeTable::getVideoId, dto.getId())
        );
        return Result.success();
    }

    @Async
    @Operation(summary = "获取视频")
    @GetMapping("")
    public CompletableFuture<Result<VideoForceWithWillUpdateTimeVO>> getVideo(@RequestParam Long id) {
        var video = videoMapper.getBigVideo(id);
        if (video == null) throw new BaseException(MessageConstant.VIDEO_NOT_FOUND);
        return CompletableFuture.completedFuture(Result.success(video));
    }

    @Operation(summary = "批量改视频index")
    @PostMapping("update-many-index")
    public Result<String> updateManyIndex(@RequestBody @Valid UpdateManyVideoIndexDTO dto) {
        videoService.updateManyIndex(dto.getArr());
        return Result.success();
    }
}
