package com.abdecd.moebackend.business.service.videogroup;

import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoMapper;
import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.pojo.dto.videogroup.PlainVideoGroupAddDTO;
import com.abdecd.moebackend.business.pojo.dto.videogroup.PlainVideoGroupUpdateDTO;
import com.abdecd.moebackend.business.pojo.vo.plainuser.UploaderVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.ContentsItemVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.PlainVideoGroupVO;
import com.abdecd.moebackend.business.service.fileservice.FileService;
import com.abdecd.moebackend.business.service.plainuser.PlainUserService;
import com.abdecd.moebackend.business.service.search.SearchService;
import com.abdecd.moebackend.business.service.video.VideoService;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PlainVideoGroupServiceBase {
    @Autowired
    private VideoGroupMapper videoGroupMapper;
    @Autowired
    private PlainUserService plainUserService;
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private VideoService videoService;
    @Autowired
    private FileService fileService;
    @Autowired
    private SearchService searchService;

    @Cacheable(cacheNames = RedisConstant.VIDEO_GROUP_CACHE, key = "#videoGroupId", unless = "#result == null")
    public PlainVideoGroupVO getVideoGroupInfo(Long videoGroupId) {
        var base = videoGroupMapper.selectById(videoGroupId);
        if (base == null || !Objects.equals(base.getVideoGroupStatus(), VideoGroup.Status.ENABLE) || !Objects.equals(base.getType(), VideoGroup.Type.PLAIN_VIDEO_GROUP)) return null;
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

        var vo = new PlainVideoGroupVO();
        BeanUtils.copyProperties(base, vo);
        vo.setUploader(uploaderVO);
        return vo;
    }
    public PlainVideoGroupVO getVideoGroupInfoForce(Long videoGroupId) {
        var base = videoGroupMapper.selectById(videoGroupId);
        if (base == null || !Objects.equals(base.getType(), VideoGroup.Type.PLAIN_VIDEO_GROUP)) return null;
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

        var vo = new PlainVideoGroupVO();
        BeanUtils.copyProperties(base, vo);
        vo.setUploader(uploaderVO);
        return vo;
    }

    @Cacheable(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, key = "#videoGroupId", unless = "#result == null")
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

    @Transactional
    public Long addVideoGroup(PlainVideoGroupAddDTO plainVideoGroupAddDTO) {
        var entity = plainVideoGroupAddDTO.toEntity(UserContext.getUserId());
        videoGroupMapper.insert(entity);

        // 封面处理
        var coverUrl = plainVideoGroupAddDTO.getCover();
        try {
            var cover = fileService.changeTmpFileToStatic(
                    coverUrl,
                    "/video-group/" + entity.getId(),
                    "cover" + coverUrl.substring(coverUrl.lastIndexOf('.'))
            );
            if (cover.isEmpty()) throw new Exception(); // will be caught
            videoGroupMapper.updateById(entity.setCover(cover));
        } catch (Exception e) {
            throw new BaseException(MessageConstant.INVALID_FILE_PATH);
        }

        return entity.getId();
    }

    @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CACHE, beforeInvocation = true, key = "#plainVideoGroupUpdateDTO.id")
    @Transactional
    public void updateVideoGroup(PlainVideoGroupUpdateDTO plainVideoGroupUpdateDTO, Byte videoGroupStatus) {
        checkUserHaveTheGroup(plainVideoGroupUpdateDTO.getId());

        var entity = plainVideoGroupUpdateDTO.toEntity(UserContext.getUserId());
        if (videoGroupStatus != null) entity.setVideoGroupStatus(videoGroupStatus);
        videoGroupMapper.updateById(entity);

        // 封面处理
        var coverUrl = plainVideoGroupUpdateDTO.getCover();
        if (coverUrl != null) {
            try {
                var cover = fileService.changeTmpFileToStatic(
                        coverUrl,
                        "/video-group/" + entity.getId(),
                        "cover" + coverUrl.substring(coverUrl.lastIndexOf('.'))
                );
                if (cover.isEmpty()) throw new Exception(); // will be caught
                videoGroupMapper.updateById(entity.setCover(cover));
            } catch (Exception e) {
                throw new BaseException(MessageConstant.INVALID_FILE_PATH);
            }
        }

        // 更新es
        var self = SpringContextUtil.getBean(PlainVideoGroupServiceBase.class);
        var vo = self.getVideoGroupInfo(plainVideoGroupUpdateDTO.getId());
        if (vo != null) {
            searchService.saveSearchEntity(vo);
        } else searchService.deleteSearchEntity(plainVideoGroupUpdateDTO.getId());
    }

    @Async("slowExecutor")
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CACHE, key = "#videoGroupId"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, key = "#videoGroupId")
    })
    @Transactional
    public void deleteVideoGroup(Long videoGroupId) {
        checkUserHaveTheGroup(videoGroupId);
        videoGroupMapper.deleteById(videoGroupId);
        // 删视频
        for (var video : videoMapper.selectList(new LambdaQueryWrapper<Video>().eq(Video::getVideoGroupId, videoGroupId)))
            videoService.deleteVideo(video.getId());
        // 删文件夹
        fileService.deleteDirInSystem("/video-group/" + videoGroupId);
        // 删es
        searchService.deleteSearchEntity(videoGroupId);
    }

    public boolean checkAddVideoGroupPending(Long id) {
        var self = SpringContextUtil.getBean(PlainVideoGroupServiceBase.class);
        var contents = self.getContents(id);
        if (contents == null || contents.isEmpty()) return false;
        return videoService.checkVideoPending(contents.getFirst().getVideoId());
    }

    /**
     * 检验空值以及是否是当前用户的视频组
     * @param videoGroupId :
     */
    public void checkUserHaveTheGroup(Long videoGroupId) {
        var videoGroupService = SpringContextUtil.getBean(VideoGroupServiceBase.class);
        videoGroupService.checkUserHaveTheGroup(videoGroupId);
    }
}