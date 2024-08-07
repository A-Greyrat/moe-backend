package com.abdecd.moebackend.business.service;

import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.dao.entity.BangumiTimeTable;
import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.BangumiTimeTableMapper;
import com.abdecd.moebackend.business.pojo.dto.video.AddVideoFullDTO;
import com.abdecd.moebackend.business.pojo.dto.video.UpdateVideoFullDTO;
import com.abdecd.moebackend.business.pojo.vo.video.VideoForceVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.BangumiVideoGroupTimeScheduleVO;
import com.abdecd.moebackend.business.service.video.VideoService;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.constant.StatusConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class BangumiTimeTableService {
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;
    @Autowired
    private VideoService videoService;
    @Autowired
    private BangumiTimeTableMapper bangumiTimeTableMapper;

    public void addVideoCb(AddVideoFullDTO addVideoDTO, long id) {
        // 只对番剧生效
        if (Objects.equals(videoGroupServiceBase.getVideoGroupType(addVideoDTO.getVideoGroupId()), VideoGroup.Type.ANIME_VIDEO_GROUP)) {
            if (
                Objects.equals(addVideoDTO.getVideoStatusWillBe(), Video.Status.PRELOAD)
                    && addVideoDTO.getVideoPublishTime() != null
            ) {
                bangumiTimeTableMapper.insert(new BangumiTimeTable()
                    .setVideoId(id)
                    .setVideoGroupId(addVideoDTO.getVideoGroupId())
                    .setUpdateTime(addVideoDTO.getVideoPublishTime())
                    .setStatus(addVideoDTO.getLink().isEmpty() ? StatusConstant.DISABLE : StatusConstant.ENABLE) // 没有链接就设为已处理，但也录入
                );
            }
        }
    }

    public void updateVideoCb(UpdateVideoFullDTO dto, VideoForceVO video) {
        if (Objects.equals(videoGroupServiceBase.getVideoGroupType(dto.getVideoGroupId()), VideoGroup.Type.ANIME_VIDEO_GROUP)) {
            // 还是 preload 就设置为新设的时间
            if (
                Objects.equals(dto.getVideoStatusWillBe(), Video.Status.PRELOAD)
                    && dto.getVideoPublishTime() != null
            ) {
                if (bangumiTimeTableMapper.update(new LambdaUpdateWrapper<BangumiTimeTable>()
                    .eq(BangumiTimeTable::getVideoId, video.getId())
                    .set(BangumiTimeTable::getVideoGroupId, video.getVideoGroupId())
                    .set(BangumiTimeTable::getUpdateTime, dto.getVideoPublishTime())
                    .set(BangumiTimeTable::getStatus, video.getSrc().isEmpty() ? StatusConstant.DISABLE : StatusConstant.ENABLE)
                ) == 0) {
                    bangumiTimeTableMapper.insert(new BangumiTimeTable()
                        .setVideoId(video.getId())
                        .setVideoGroupId(video.getVideoGroupId())
                        .setUpdateTime(dto.getVideoPublishTime())
                        .setStatus(video.getSrc().isEmpty() ? StatusConstant.DISABLE : StatusConstant.ENABLE)
                    );
                }
            }
            // 是 enable 就删除可能存在的预发布状态
            if (Objects.equals(dto.getVideoStatusWillBe(), Video.Status.ENABLE)) {
                bangumiTimeTableMapper.delete(new LambdaQueryWrapper<BangumiTimeTable>()
                    .eq(BangumiTimeTable::getVideoId, video.getId())
                );
            }
        }
    }

    @Cacheable(cacheNames = RedisConstant.BANGUMI_TIME_SCHEDULE_CACHE, key = "#date", unless = "#result == null")
    public List<BangumiVideoGroupTimeScheduleVO> getTimeSchedule(LocalDate date) {
        if (date.isBefore(LocalDate.now().minusDays(1))) return null;
        if (date.isAfter(LocalDate.now().plusDays(6))) return null;

        var objs = bangumiTimeTableMapper.selectList(new LambdaQueryWrapper<BangumiTimeTable>()
            .ge(BangumiTimeTable::getUpdateTime, date.atStartOfDay())
            .lt(BangumiTimeTable::getUpdateTime, date.plusDays(1).atStartOfDay())
            .last("ORDER BY update_time ASC limit 20")
        );
        if (objs.isEmpty()) return new ArrayList<>();
        var videoGroupServiceBase = SpringContextUtil.getBean(VideoGroupServiceBase.class);
        return new ArrayList<>(objs.stream()
            .map(obj -> {
                var videoGroup = videoGroupServiceBase.getVideoGroupWithData(obj.getVideoGroupId());
                if (videoGroup == null) return null;
                var video = videoService.getVideoForce(obj.getVideoId());
                return new BangumiVideoGroupTimeScheduleVO()
                    .setVideoGroupWithDataVO(videoGroup)
                    .setWillUpdateTime(obj.getUpdateTime())
                    .setWillUpdateIndex(video.getIndex())
                    .setWillUpdateTitle(video.getTitle());
            })
            .filter(Objects::nonNull)
            .toList()
        );
    }
}
