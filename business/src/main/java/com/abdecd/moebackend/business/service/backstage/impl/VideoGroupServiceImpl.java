package com.abdecd.moebackend.business.service.backstage.impl;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.dao.entity.PlainUserDetail;
import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.PlainUserDetailMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoMapper;
import com.abdecd.moebackend.business.pojo.dto.backstage.bangumiVideoGroup.BangumiVideoGroupUpdateDTO;
import com.abdecd.moebackend.business.pojo.dto.backstage.commonVideoGroup.VideoGroupDTO;
import com.abdecd.moebackend.business.pojo.vo.backstage.commonVideoGroup.VideoGroupListVO;
import com.abdecd.moebackend.business.pojo.vo.backstage.commonVideoGroup.VideoGroupVO;
import com.abdecd.moebackend.business.pojo.vo.backstage.commonVideoGroup.VideoVo;
import com.abdecd.moebackend.business.pojo.vo.plainuser.UploaderVO;
import com.abdecd.moebackend.business.pojo.vo.statistic.StatisticDataVO;
import com.abdecd.moebackend.business.service.backstage.VideoGroupService;
import com.abdecd.moebackend.business.service.fileservice.FileService;
import com.abdecd.moebackend.business.service.search.SearchService;
import com.abdecd.moebackend.business.service.statistic.StatisticService;
import com.abdecd.moebackend.business.service.video.VideoService;
import com.abdecd.moebackend.business.service.videogroup.BangumiVideoGroupServiceBase;
import com.abdecd.moebackend.business.service.videogroup.PlainVideoGroupServiceBase;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.constant.VideoGroupConstant;
import com.abdecd.tokenlogin.common.context.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

@Service
@Slf4j
public class VideoGroupServiceImpl implements VideoGroupService {
    @Resource
    private VideoGroupMapper videoGroupMapper;

    @Resource
    private FileService fileService;

    @Resource
    private VideoMapper videoMapper;

    @Resource
    private StatisticService statisticService;

    @Resource
    private PlainUserDetailMapper plainUserDetailMapper;

    @Resource
    private VideoService videoService;

    @Resource
    private SearchService searchService;

    @Resource
    private VideoGroupServiceBase videoGroupServiceBase;

    @Resource
    private PlainVideoGroupServiceBase plainVideoGroupServiceBase;

    @Resource
    private BangumiVideoGroupServiceBase bangumiVideoGroupServiceBase;


    @Transactional
    @Override
    public Long insert(VideoGroup videoGroup, MultipartFile cover) {
        Long uid = UserContext.getUserId();

        String coverPath = "";

        videoGroup.setCover(coverPath)
                .setUserId(uid)
                .setWeight(VideoGroupConstant.DEFAULT_WEIGHT)
                .setType(VideoGroupConstant.COMMON_VIDEO_GROUP);

        videoGroupMapper.insertVideoGroup(videoGroup);

        try {
            //TODO 文件没有存下来
            String coverName = cover.getOriginalFilename().substring(0, cover.getOriginalFilename().lastIndexOf("."));
            String coverPath_ = "/video-group/" + videoGroup.getId();
            coverPath = fileService.uploadFile(cover, coverPath_, coverName + ".jpg");
        } catch (IOException e) {
            throw new BaseException("文件存储失败");
        }
        videoGroup.setCover(coverPath);

        videoGroupMapper.update_(videoGroup);

        var vo = getVOinfo(videoGroup.getId());
        if (vo != null)
            searchService.saveSearchEntity(vo);

        return videoGroup.getId();
    }

    private com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupVO getVOinfo(Long id) {
        var type = videoGroupServiceBase.getVideoGroupType(id);

        if (Objects.equals(type, VideoGroup.Type.PLAIN_VIDEO_GROUP)) {
            return plainVideoGroupServiceBase.getVideoGroupInfo(id);
        } else if (Objects.equals(type, VideoGroup.Type.ANIME_VIDEO_GROUP)) {
            return bangumiVideoGroupServiceBase.getVideoGroupInfo(id);
        } else return null;
    }

    @Override
    public void delete(Long id) {
        VideoGroup videoGroup = new VideoGroup();
        videoGroup.setId(id);
        videoGroupMapper.deleteById(videoGroup);

        searchService.deleteSearchEntity(id);
    }

    @Override
    public void update(VideoGroupDTO videoGroupDTO) {
        String coverPath = null;

        if (videoGroupDTO.getCover() != null) {
            try {
                //TODO 文件没有存下来
                String coverName = videoGroupDTO.getCover().getOriginalFilename().substring(0, videoGroupDTO.getCover().getOriginalFilename().lastIndexOf("."));
                String coverPath_ = "/video-group/" + videoGroupDTO.getId();
                coverPath = fileService.uploadFile(videoGroupDTO.getCover(), coverPath_, coverName + ".jpg");
            } catch (IOException e) {
                throw new BaseException("文件存储失败");
            }
        }

        VideoGroup videoGroup = new VideoGroup();
        BeanUtils.copyProperties(videoGroupDTO, videoGroup);
        if (coverPath != null)
            videoGroup.setCover(coverPath);

        videoGroupMapper.update_(videoGroup);
        var newOne = videoGroupServiceBase.getVideoGroupInfo(videoGroup.getId());

        if (newOne != null) {
            searchService.saveSearchEntity(newOne);
        } else {
            searchService.deleteSearchEntity(videoGroup.getId());
        }
    }

    @Override
    public VideoGroupVO getById(Long id) {
        VideoGroupVO videoGroupVO = new VideoGroupVO();
        VideoGroup videoGroup = videoGroupMapper.selectById(id);
        if (videoGroup == null) {
            throw new BaseException("视频组缺失");
        }

        videoGroupVO.setId(id);
        videoGroupVO.setCover(videoGroup.getCover());
        videoGroupVO.setDescription(videoGroup.getDescription());
        videoGroupVO.setTitle(videoGroup.getTitle());
        videoGroupVO.setType(videoGroup.getType());

        videoGroupVO.setTags(videoGroup.getTags());
        videoGroupVO.setCreateTime(String.valueOf(videoGroup.getCreateTime()));
        videoGroupVO.setWeight(videoGroup.getWeight());

        UploaderVO uploaderVO = new UploaderVO();
        uploaderVO.setId(videoGroup.getUserId());
        PlainUserDetail plainUserDetail = plainUserDetailMapper.selectByUid(videoGroup.getUserId());
        if (plainUserDetail != null) {
            uploaderVO.setAvatar(plainUserDetail.getAvatar());
            uploaderVO.setNickname(plainUserDetail.getNickname());
        }

        videoGroupVO.setUploader(uploaderVO);

        return videoGroupVO;
    }

    @Override
    public ArrayList<VideoVo> getContentById(Long id) {
        ArrayList<VideoVo> videoVOList = new ArrayList<>();
        ArrayList<Video> videoList = videoMapper.getByGroupid(id);

        for (Video video : videoList) {
            VideoVo videoVo = new VideoVo();

            videoVo.setVideoId(String.valueOf(video.getId()));
            videoVo.setVideoCover(video.getCover());
            videoVo.setIndex(String.valueOf(video.getIndex()));
            videoVo.setTitle(video.getTitle());
            videoVo.setVideoGroupId(String.valueOf(id));

            videoVOList.add(videoVo);
        }

        return videoVOList;
    }

    @Override
    public Byte getTypeByVideoId(Long id) {
        VideoGroup videoGroup = videoGroupMapper.selectById(id);
        return videoGroup.getType();
    }

    @Override
    public VideoGroupListVO getVideoGroupList(Integer page, Integer pageSize) {
        VideoGroupListVO videoGroupListVO = new VideoGroupListVO();
        videoGroupListVO.setRecords(new ArrayList<>());

        Integer offset = (page - 1) * pageSize;
        ArrayList<VideoGroup> videoGroups = videoGroupMapper.selectbyPage(offset, pageSize);

        for (VideoGroup videoGroup : videoGroups) {
            VideoGroupVO videoGroupVO = new VideoGroupVO();

            videoGroupVO.setId(videoGroup.getId());
            videoGroupVO.setCover(videoGroup.getCover());
            videoGroupVO.setDescription(videoGroup.getDescription());
            videoGroupVO.setTitle(videoGroup.getTitle());
            videoGroupVO.setType(videoGroup.getType());
            videoGroupVO.setCreateTime(String.valueOf(videoGroup.getCreateTime()));

            videoGroupVO.setTags(videoGroup.getTags());

            UploaderVO uploaderVO = new UploaderVO();
            uploaderVO.setId(videoGroup.getUserId());
            PlainUserDetail plainUserDetail = plainUserDetailMapper.selectByUid(videoGroup.getUserId());
            if (plainUserDetail != null) {
                uploaderVO.setAvatar(plainUserDetail.getAvatar());
                uploaderVO.setNickname(plainUserDetail.getNickname());
            }

            videoGroupVO.setUploader(uploaderVO);

            StatisticDataVO statisticDataVO = statisticService.getStatisticData(videoGroupVO.getId());
            videoGroupVO.setWatchCnt(Math.toIntExact(statisticDataVO.getWatchCnt()));
            videoGroupVO.setFavoriteCnt(Math.toIntExact(statisticDataVO.getFavoriteCnt()));
            videoGroupVO.setLikeCnt(Math.toIntExact(statisticDataVO.getLikeCnt()));
            videoGroupVO.setUserLike(statisticDataVO.getUserLike());
            videoGroupVO.setUserFavorite(statisticDataVO.getUserFavorite());
            videoGroupVO.setCommentCnt(Math.toIntExact(statisticDataVO.getCommentCnt()));
            videoGroupVO.setDanmakuCnt(Math.toIntExact(statisticDataVO.getDanmakuCnt()));

            videoGroupListVO.getRecords().add(videoGroupVO);
        }

        videoGroupListVO.setTotal(videoGroupListVO.getRecords().size());
        return videoGroupListVO;
    }

    @Override
    public void update(@Valid BangumiVideoGroupUpdateDTO videoGroup) {
        String coverPath = null;

        if (videoGroup.getCover() != null) {
            try {
                //TODO 文件没有存下来
                String coverName = videoGroup.getCover().getOriginalFilename().substring(0, videoGroup.getCover().getOriginalFilename().lastIndexOf("."));
                String coverPath_ = "/video-group/" + videoGroup.getId();
                coverPath = fileService.uploadFile(videoGroup.getCover(), coverPath_, coverName + ".jpg");
            } catch (IOException e) {
                throw new BaseException("文件存储失败");
            }
        }

        var entity = new VideoGroup()
                .setId(videoGroup.getId())
                .setVideoGroupStatus(videoGroup.getVideoGroupStatus() != null ? Byte.valueOf(videoGroup.getVideoGroupStatus()) : null)
                .setTitle(videoGroup.getTitle())
                .setCover(coverPath)
                .setDescription(videoGroup.getDescription())
                .setWeight(videoGroup.getWeight())
                .setTags(videoGroup.getTags());
        videoGroupMapper.update_(entity);

        var newOne = videoGroupServiceBase.getVideoGroupInfo(entity.getId());
        if (newOne != null) {
            searchService.saveSearchEntity(newOne);
        } else {
            searchService.deleteSearchEntity(entity.getId());
        }
    }

    @Async("slowExecutor")
    @Override
    public void deleteVideoGroup(Long id) {
        videoGroupMapper.deleteById(id);
        // 删视频
        for (var video : videoMapper.selectList(new LambdaQueryWrapper<Video>().eq(Video::getVideoGroupId, id)))
            videoService.deleteVideo(video.getId());
        // 删文件夹
        fileService.deleteDirInSystem("/video-group/" + id);
        // 删es
        searchService.deleteSearchEntity(id);
    }

    @Caching(evict = {
            @CacheEvict(value = RedisConstant.VIDEO_GROUP_CACHE, key = "#videoGroupId"),
            @CacheEvict(value = RedisConstant.BANGUMI_VIDEO_GROUP_CACHE, key = "#videoGroupId")
    })
    @Override
    public Integer changeStatus(Long videoGroupId, Integer status) {
        var result = videoGroupMapper.updateStatus(videoGroupId, status);
        var newOne = videoGroupServiceBase.getVideoGroupInfoForce(videoGroupId);
        if (newOne == null || !Objects.equals(newOne.getVideoGroupStatus(), VideoGroup.Status.ENABLE)) {
            searchService.deleteSearchEntity(videoGroupId);
        } else {
            searchService.saveSearchEntity(newOne);
        }
        return result;
    }
}
