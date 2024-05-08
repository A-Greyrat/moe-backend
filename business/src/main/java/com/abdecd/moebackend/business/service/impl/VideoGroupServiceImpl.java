package com.abdecd.moebackend.business.service.impl;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.dao.entity.PlainUserDetail;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.entity.VideoGroupTag;
import com.abdecd.moebackend.business.dao.mapper.PlainUserDetailMapper;
import com.abdecd.moebackend.business.dao.mapper.VIdeoGroupMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupAndTagMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupTagMapper;
import com.abdecd.moebackend.business.pojo.dto.commonVideoGroup.VIdeoGroupDTO;
import com.abdecd.moebackend.business.pojo.vo.common.UploaderVO;
import com.abdecd.moebackend.business.pojo.vo.common.VideoGroupVO;
import com.abdecd.moebackend.business.service.FileService;
import com.abdecd.moebackend.business.service.VIdeoGroupService;
import com.abdecd.moebackend.common.constant.VideoGroupConstant;
import com.abdecd.tokenlogin.common.context.UserContext;
import com.aliyun.oss.model.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

@Service
@Slf4j
public class VideoGroupServiceImpl implements VIdeoGroupService {
    @Resource
    private VIdeoGroupMapper vIdeoGroupMapper;

    @Resource
    private FileService fileService;

    @Resource
    private VideoGroupTagMapper videoGroupTagMapper;

    @Resource
    private PlainUserDetailMapper plainUserDetailMapper;

    @Resource
    private VideoGroupAndTagMapper videoGroupandTagMapper;

    @Override
    public Long insert(VIdeoGroupDTO videoGroupDTO) {
        Long uid = UserContext.getUserId();

        String coverPath;

        try {
            //TODO 文件没有存下来
            String randomImageName = UUID.randomUUID().toString() + ".jpg";
            coverPath =  fileService.uploadFile(videoGroupDTO.getCover(),randomImageName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        VideoGroup videoGroup = new VideoGroup();
        videoGroup.setUserId(uid)
                .setTitle(videoGroupDTO.getTitle())
                .setDescription(videoGroupDTO.getDescription())
                .setCover(coverPath)
                .setCreate_time(videoGroupDTO.getDate())
                .setWeight(VideoGroupConstant.DEFAULT_WEIGHT)
                .setType(VideoGroupConstant.COMMON_VIDEO_GROUP);

        vIdeoGroupMapper.insertVideoGroup(videoGroup);

        return  videoGroup.getId();
    }

    @Override
    public void delete(Long id) {
        VideoGroup videoGroup = new VideoGroup();
        videoGroup.setId(id);
        vIdeoGroupMapper.deleteById(videoGroup);
    }

    @Override
    public VideoGroupVO update(VIdeoGroupDTO videoGroupDTO) {
        String coverPath = new String();

        if(videoGroupDTO.getCover() != null)
        {
            try {
                //TODO 文件没有存下来
                String randomImageName = UUID.randomUUID().toString() + ".jpg";
                coverPath =  fileService.uploadFile(videoGroupDTO.getCover(),randomImageName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        VideoGroup videoGroup = new VideoGroup();
        BeanUtils.copyProperties(videoGroupDTO,videoGroup);
        videoGroup.setCover(coverPath);

        vIdeoGroupMapper.update(videoGroup);
        return null;
    }

    @Override
    public VideoGroupVO getById(Long id) {
        VideoGroupVO videoGroupVO = new VideoGroupVO();
        VideoGroup videoGroup = vIdeoGroupMapper.selectById(id);
        if(videoGroup == null)
        {
            throw new BaseException("视频组缺失");
        }
        videoGroupVO.setVideoGroupId(id);
        videoGroupVO.setCover(videoGroup.getCover());
        videoGroupVO.setDescription(videoGroup.getDescription());
        videoGroupVO.setTitle(videoGroup.getTitle());
        videoGroupVO.setType(videoGroup.getType());

        ArrayList<Long> tagIds = videoGroupandTagMapper.selectByVid(id);
        ArrayList<VideoGroupTag> videoGroupTagList = new ArrayList<>();

        for (Long id_ : tagIds) {
            VideoGroupTag tag = videoGroupTagMapper.selectById(id_);
            videoGroupTagList.add(tag);
            log.info("tag:              {}",tag);
        }

        videoGroupVO.setTags(videoGroupTagList);

        UploaderVO uploaderVO = new UploaderVO();
        uploaderVO.setId(videoGroup.getUserId());
        PlainUserDetail plainUserDetail =  plainUserDetailMapper.selectByUid(videoGroup.getUserId());
        if(plainUserDetail != null){
            uploaderVO.setAvatar(plainUserDetail.getAvatar());
            uploaderVO.setNickname(plainUserDetail.getNickname());
        }

        videoGroupVO.setUploader(uploaderVO);

        return  videoGroupVO;
    }
}
