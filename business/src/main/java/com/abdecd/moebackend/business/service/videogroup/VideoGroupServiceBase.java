package com.abdecd.moebackend.business.service.videogroup;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.pojo.vo.videogroup.ContentsItemVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupBigVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupWithDataVO;
import com.abdecd.moebackend.business.service.ElasticSearchService;
import com.abdecd.moebackend.business.service.statistic.StatisticService;
import com.abdecd.moebackend.business.service.video.VideoService;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.result.PageVO;
import com.abdecd.tokenlogin.common.context.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    @Autowired
    private ElasticSearchService elasticSearchService;

    @Nullable
    @Cacheable(cacheNames = RedisConstant.VIDEO_GROUP_TYPE_CACHE, key = "#videoGroupId", unless = "#result == null")
    public Byte getVideoGroupType(long videoGroupId) {
        var obj = videoGroupMapper.selectById(videoGroupId);
        if (obj == null) return null;
        return obj.getType();
    }
    
    /**
     * 获取状态为已经启用的视频详情
     */
    public VideoGroupVO getVideoGroupInfo(Long videoGroupId) {
        var self = SpringContextUtil.getBean(getClass());
        var type = self.getVideoGroupType(videoGroupId);
    
        if (Objects.equals(type, VideoGroup.Type.PLAIN_VIDEO_GROUP)) {
            return plainVideoGroupServiceBase.getVideoGroupInfo(videoGroupId);
        } else if (Objects.equals(type, VideoGroup.Type.ANIME_VIDEO_GROUP)) {
            return bangumiVideoGroupServiceBase.getVideoGroupInfo(videoGroupId);
        } else return null;
    }
    
    public VideoGroupVO getVideoGroupInfoForce(Long videoGroupId) {
        var self = SpringContextUtil.getBean(getClass());
        var type = self.getVideoGroupType(videoGroupId);
    
        if (Objects.equals(type, VideoGroup.Type.PLAIN_VIDEO_GROUP)) {
            return plainVideoGroupServiceBase.getVideoGroupInfoForce(videoGroupId);
        } else if (Objects.equals(type, VideoGroup.Type.ANIME_VIDEO_GROUP)) {
            return bangumiVideoGroupServiceBase.getVideoGroupInfoForce(videoGroupId);
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
    public VideoGroupWithDataVO getVideoGroupWithDataForce(Long videoGroupId) {
        var videoGroupInfo = getVideoGroupInfoForce(videoGroupId);
        if (videoGroupInfo == null) return null;
        return new VideoGroupWithDataVO()
                .setVideoGroupVO(videoGroupInfo)
                .setStatisticDataVO(statisticService.getStatisticData(videoGroupId));
    }
    
    public VideoGroupBigVO getBigVideoGroup(Long videoGroupId) {
        var videoGroupInfo = getVideoGroupInfo(videoGroupId);
        if (videoGroupInfo == null) return null;
        var contents = getContents(videoGroupId);
        var cnts = statisticService.getFullStatisticData(videoGroupId);
        Long videoId = null;
        try {
            videoId = ((List<ContentsItemVO>) contents).getFirst().getVideoId();
        } catch (Exception ignored) {}
        if (videoId == null) videoId = -1L;
        var aVideo = videoService.getVideo(videoId);
        return new VideoGroupBigVO()
                .setVideoGroupVO(videoGroupInfo)
                .setContents(contents)
                .setStatisticDataVO(cnts)
                .setBvid(aVideo==null ? null : aVideo.getBvid())
                .setEpid(aVideo==null ? null : aVideo.getEpid());
    }
    
    public PageVO<VideoGroupWithDataVO> pageMyUploadVideoGroup(Integer page, Integer pageSize) {
        var userId = UserContext.getUserId();
        var pageObj = new Page<VideoGroup>(page, pageSize);
        var result = videoGroupMapper.selectPage(pageObj, new LambdaQueryWrapper<VideoGroup>()
                .eq(VideoGroup::getUserId, userId)
                .orderByDesc(VideoGroup::getId)
        );
        var self = SpringContextUtil.getBean(getClass());
        return new PageVO<VideoGroupWithDataVO>()
                .setTotal((int) result.getTotal())
                .setRecords(result.getRecords().stream().map(it -> self.getVideoGroupWithDataForce(it.getId())).toList()
        );
    }
    
    public List<Long> listAllAvailableVideoGroupId() {
        return videoGroupMapper.selectList(new LambdaQueryWrapper<VideoGroup>()
                .select(VideoGroup::getId)
                .eq(VideoGroup::getVideoGroupStatus, VideoGroup.Status.ENABLE)
        ).stream().map(VideoGroup::getId).toList();
    }
    
    public void changeStatus(Long videoGroupId, Byte status) {
        videoGroupMapper.updateById(new VideoGroup()
                .setId(videoGroupId)
                .setVideoGroupStatus(status)
        );
        // 改es
        if (Objects.equals(status, VideoGroup.Status.ENABLE)) {
            elasticSearchService.saveSearchEntity(getVideoGroupInfo(videoGroupId));
        } else elasticSearchService.deleteSearchEntity(videoGroupId);
    }
    
    /**
     * 检验空值以及是否是当前用户的视频组
     * @param videoGroupId :
     */
    public void checkUserHaveTheGroup(Long videoGroupId) {
        if (List.of(UserContext.getPermission().split(",")).contains("99")) return;
        var old = videoGroupMapper.selectById(videoGroupId);
        if (old == null) throw new BaseException(MessageConstant.INVALID_VIDEO_GROUP);
        if (!Optional.of(old)
                .map(VideoGroup::getUserId)
                .orElse(-1L)
                .equals(UserContext.getUserId()))
            throw new BaseException(MessageConstant.INVALID_VIDEO_GROUP);
    }
}
