package com.abdecd.moebackend.business.service.videogroup;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.pojo.vo.plainuser.UploaderVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.ContentsItemVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupBigVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupWithDataVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupVO;
import com.abdecd.moebackend.business.service.VideoService;
import com.abdecd.moebackend.business.service.statistic.StatisticService;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.tokenlogin.common.context.UserContext;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class VideoGroupServiceBase {
    @Autowired
    private VideoGroupMapper videoGroupMapper;
    @Autowired
    private PlainVideoGroupServiceBase plainVideoGroupServiceBase;
    @Autowired
    private BangumiVideoGroupServiceBase bangumiVideoGroupServiceBase;
    @Autowired
    private StatisticService statisticService;
    @Autowired
    private VideoService videoService;

    @Nullable
    @Cacheable(cacheNames = RedisConstant.VIDEO_GROUP_TYPE_CACHE, key = "#videoGroupId", unless = "#result == null")
    public Byte getVideoGroupType(long videoGroupId) {
        var obj = videoGroupMapper.selectById(videoGroupId);
        if (obj == null) return null;
        return obj.getType();
    }

    public VideoGroupVO getVideoGroupInfo(Long videoGroupId) {
        var self = SpringContextUtil.getBean(getClass());
        var type = self.getVideoGroupType(videoGroupId);

        if (Objects.equals(type, VideoGroup.Type.PLAIN_VIDEO_GROUP)) {
            return plainVideoGroupServiceBase.getVideoGroupInfo(videoGroupId);
        } else if (Objects.equals(type, VideoGroup.Type.ANIME_VIDEO_GROUP)) {
            return bangumiVideoGroupServiceBase.getVideoGroupInfo(videoGroupId);
        } else return null;
    }

    // todo 待重构
    public Object getContents(Long videoGroupId) {
        var self = SpringContextUtil.getBean(getClass());
        var type = self.getVideoGroupType(videoGroupId);

        if (Objects.equals(type, VideoGroup.Type.PLAIN_VIDEO_GROUP)) {
            return plainVideoGroupServiceBase.getContents(videoGroupId);
        } else if (Objects.equals(type, VideoGroup.Type.ANIME_VIDEO_GROUP)) {
            return bangumiVideoGroupServiceBase.getContents(videoGroupId);
        } else return null;
    }

    public VideoGroupWithDataVO getVideoGroupWithData(Long videoGroupId) {
        var videoGroupInfo = getVideoGroupInfo(videoGroupId);
        if (videoGroupInfo == null) return null;
        return new VideoGroupWithDataVO()
                .setVideoGroupVO(videoGroupInfo)
                .setStatisticDataVO(statisticService.getStatisticData(videoGroupId));
    }

    public VideoGroupBigVO getBigVideoGroup(Long videoGroupId) {
        var videoGroupInfo = getVideoGroupInfo(videoGroupId);
        if (videoGroupInfo == null) return null;
        var contents = getContents(videoGroupId);
        var cnts = statisticService.getStatisticData(videoGroupId);
        var videoId = Optional.ofNullable((List<ContentsItemVO>) contents)
                .map(List::getFirst)
                .map(ContentsItemVO::getVideoId)
                .orElse(-1L);
        var aVideo = videoService.getVideo(videoId);
        return new VideoGroupBigVO()
                .setVideoGroupVO(videoGroupInfo)
                .setContents(contents)
                .setStatisticDataVO(cnts)
                .setBvid(aVideo==null ? null : aVideo.getBvid())
                .setEpid(aVideo==null ? null : aVideo.getEpid());
    }

    /**
     * 检验空值以及是否是当前用户的视频组
     * @param videoGroupId :
     */
    public void checkUserHaveTheGroup(Long videoGroupId) {
        var old = getVideoGroupInfo(videoGroupId);
        if (old == null) throw new BaseException(MessageConstant.INVALID_VIDEO_GROUP);
        if (!Optional.of(old)
                .map(VideoGroupVO::getUploader)
                .map(UploaderVO::getId).orElse(-1L).equals(UserContext.getUserId()))
            throw new BaseException(MessageConstant.INVALID_VIDEO_GROUP);
    }
}