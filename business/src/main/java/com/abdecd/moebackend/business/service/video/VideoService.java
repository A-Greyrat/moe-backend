package com.abdecd.moebackend.business.service.video;

import com.abdecd.moebackend.business.pojo.dto.video.AddVideoDTO;
import com.abdecd.moebackend.business.pojo.dto.video.UpdateManyVideoIndexDTO;
import com.abdecd.moebackend.business.pojo.dto.video.UpdateVideoDTO;
import com.abdecd.moebackend.business.pojo.vo.video.VideoForceVO;
import com.abdecd.moebackend.business.pojo.vo.video.VideoVO;

import java.util.List;

public interface VideoService {
    long addVideo(AddVideoDTO addVideoDTO, Byte videoStatusWillBe);
    long addVideo(AddVideoDTO addVideoDTO, Byte videoStatusWillBe, boolean check);
    boolean checkVideoPending(Long videoId);
    void updateVideo(UpdateVideoDTO updateVideoDTO, Byte videoStatusWillBe);
    void deleteVideo(Long videoId);
    VideoVO getVideo(Long videoId);
    VideoForceVO getVideoForce(Long videoId);
    VideoVO getVideoBase(Long videoId);
    void setVideoSrcTo(VideoVO vo);
    void videoStatusUpdate(Long videoId, Byte videoStatus);

    void updateManyIndex(List<UpdateManyVideoIndexDTO.UpdateVideoIndexDTO> arr);
}
