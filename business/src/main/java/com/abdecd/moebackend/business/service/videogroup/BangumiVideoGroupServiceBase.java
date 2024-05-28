package com.abdecd.moebackend.business.service.videogroup;

import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.dao.entity.BangumiTimeTable;
import com.abdecd.moebackend.business.dao.entity.BangumiVideoGroup;
import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.BangumiTimeTableMapper;
import com.abdecd.moebackend.business.dao.mapper.BangumiVideoGroupMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoMapper;
import com.abdecd.moebackend.business.pojo.vo.plainuser.UploaderVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.BangumiVideoGroupTimeScheduleVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.BangumiVideoGroupVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.ContentsItemVO;
import com.abdecd.moebackend.business.service.plainuser.PlainUserService;
import com.abdecd.moebackend.business.service.video.VideoService;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class BangumiVideoGroupServiceBase {
    @Autowired
    private VideoGroupMapper videoGroupMapper;
    @Autowired
    private BangumiVideoGroupMapper bangumiVideoGroupMapper;
    @Autowired
    private PlainUserService plainUserService;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private VideoService videoService;
    @Autowired
    private BangumiTimeTableMapper bangumiTimeTableMapper;

    @Cacheable(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CACHE, key = "#videoGroupId", unless = "#result == null")
    public BangumiVideoGroupVO getVideoGroupInfo(Long videoGroupId) {
        var base = videoGroupMapper.selectById(videoGroupId);
        if (base == null || !Objects.equals(base.getVideoGroupStatus(), VideoGroup.Status.ENABLE) || !Objects.equals(base.getType(), VideoGroup.Type.ANIME_VIDEO_GROUP)) return null;
        var appendix = bangumiVideoGroupMapper.selectOne(new LambdaQueryWrapper<BangumiVideoGroup>()
                .eq(BangumiVideoGroup::getVideoGroupId, videoGroupId)
        );
        if (appendix == null) return null;
        // 为空是管理员
        var uploader = plainUserService.getPlainUserDetail(base.getUserId());
        var uploaderVO = uploader == null
                ? new UploaderVO()
                .setId(null)
                .setNickname(MessageConstant.ADMIN)
                .setAvatar(MessageConstant.ADMIN_AVATAR)
                : new UploaderVO()
                .setId(uploader.getUserId())
                .setNickname(uploader.getNickname())
                .setAvatar(uploader.getAvatar());

        var vo = new BangumiVideoGroupVO();
        BeanUtils.copyProperties(base, vo);
        BeanUtils.copyProperties(appendix, vo);
        vo.setUploader(uploaderVO);
        return vo;
    }
    public BangumiVideoGroupVO getVideoGroupInfoForce(Long videoGroupId) {
        var base = videoGroupMapper.selectById(videoGroupId);
        if (base == null || !Objects.equals(base.getType(), VideoGroup.Type.ANIME_VIDEO_GROUP)) return null;
        var appendix = bangumiVideoGroupMapper.selectOne(new LambdaQueryWrapper<BangumiVideoGroup>()
                .eq(BangumiVideoGroup::getVideoGroupId, videoGroupId)
        );
        if (appendix == null) return null;
        // 为空是管理员
        var uploader = plainUserService.getPlainUserDetail(base.getUserId());
        var uploaderVO = uploader == null
                ? new UploaderVO()
                .setId(null)
                .setNickname(MessageConstant.ADMIN)
                .setAvatar(MessageConstant.ADMIN_AVATAR)
                : new UploaderVO()
                .setId(uploader.getUserId())
                .setNickname(uploader.getNickname())
                .setAvatar(uploader.getAvatar());

        var vo = new BangumiVideoGroupVO();
        BeanUtils.copyProperties(base, vo);
        BeanUtils.copyProperties(appendix, vo);
        vo.setUploader(uploaderVO);
        return vo;
    }

    @Cacheable(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, key = "#videoGroupId", unless = "#result == null")
    public List<ContentsItemVO> getContents(Long videoGroupId) {
        var videoList = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .eq(Video::getVideoGroupId, videoGroupId)
                .eq(Video::getStatus, Video.Status.ENABLE)
        );
        return new ArrayList<>(
                videoList.stream().map(video -> new ContentsItemVO()
                        .setVideoId(video.getId())
                        .setVideoGroupId(video.getVideoGroupId())
                        .setIndex(video.getIndex())
                        .setTitle(video.getTitle())
                        .setVideoCover(video.getCover())
                ).toList()
        );
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
