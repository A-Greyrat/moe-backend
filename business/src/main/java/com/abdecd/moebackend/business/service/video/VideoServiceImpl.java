package com.abdecd.moebackend.business.service.video;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.common.property.MoeProperties;
import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.dao.entity.Danmaku;
import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.entity.VideoSrc;
import com.abdecd.moebackend.business.dao.mapper.DanmakuMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoSrcMapper;
import com.abdecd.moebackend.business.lib.BiliParser;
import com.abdecd.moebackend.business.lib.ResourceLinkHandler;
import com.abdecd.moebackend.business.pojo.dto.video.AddVideoDTO;
import com.abdecd.moebackend.business.pojo.dto.video.UpdateManyVideoIndexDTO;
import com.abdecd.moebackend.business.pojo.dto.video.UpdateVideoDTO;
import com.abdecd.moebackend.business.pojo.dto.video.VideoTransformTask;
import com.abdecd.moebackend.business.pojo.vo.video.VideoForceVO;
import com.abdecd.moebackend.business.pojo.vo.video.VideoSrcVO;
import com.abdecd.moebackend.business.pojo.vo.video.VideoVO;
import com.abdecd.moebackend.business.service.fileservice.FileService;
import com.abdecd.moebackend.business.service.plainuser.PlainUserService;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.tokenlogin.common.context.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Slf4j
@Service
public class VideoServiceImpl implements VideoService {
    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private VideoSrcMapper videoSrcMapper;
    @Autowired
    private VideoTransformer videoTransformer;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ResourceLinkHandler resourceLinkHandler;
    @Autowired
    private FileService fileService;
    @Autowired
    private BiliParser biliParser;
    @Autowired
    private PlainUserService plainUserService;
    @Autowired
    private MoeProperties moeProperties;
    @Autowired
    private VideoGroupMapper videoGroupMapper;
    @Autowired
    private DanmakuMapper danmakuMapper;

    private static final Pattern bvPattern = Pattern.compile("BV[a-zA-Z0-9]+");

    private static final int TRANSFORM_TASK_TTL = 1800;

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, key = "#addVideoDTO.videoGroupId"),
            @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, key = "#addVideoDTO.videoGroupId"),
    })
    @Transactional
    @Override
    public long addVideo(AddVideoDTO addVideoDTO, Byte videoStatusWillBe) {
        checkUserHaveTheGroup(addVideoDTO.getVideoGroupId());

        var entity = addVideoDTO.toEntity();
        videoMapper.insert(entity);

        // 封面处理
        var coverUrl = addVideoDTO.getCover();
        try {
            var cover = fileService.changeTmpFileToStatic(
                    coverUrl,
                    "/video-group/" + entity.getVideoGroupId() + "/" + entity.getId(),
                    "cover" + coverUrl.substring(coverUrl.lastIndexOf('.'))
            );
            if (cover.isEmpty()) throw new Exception(); // will be caught
            videoMapper.updateById(entity.setCover(cover));
        } catch (Exception e) {
            throw new BaseException(MessageConstant.INVALID_FILE_PATH);
        }
        // 链接处理
        if (addVideoDTO.getLink() != null && !addVideoDTO.getLink().isEmpty()) {
            var originPath = resourceLinkHandler.getRawPathFromTmpVideoLink(addVideoDTO.getLink());
            // 链接合法性检查 不是bv就正常检查
            if (!bvPattern.matcher(addVideoDTO.getLink()).find()) {
                if (!originPath.startsWith("tmp/user" + UserContext.getUserId() + "/"))
                    throw new BaseException(MessageConstant.INVALID_FILE_PATH);
            }
            if (bvPattern.matcher(addVideoDTO.getLink()).find()) {
                videoMapper.updateById(entity.setStatus(videoStatusWillBe));
                videoSrcMapper.insert(new VideoSrc().setVideoId(entity.getId()).setSrcName("720p").setSrc(addVideoDTO.getLink()));
            } else if (Objects.equals(videoStatusWillBe, Video.Status.ENABLE)) {
                createTransformTask(entity.getVideoGroupId(), entity.getId(), originPath, "videoServiceImpl.videoTransformEnableCb", "videoServiceImpl.videoTransformFailCb");
            } else if (Objects.equals(videoStatusWillBe, Video.Status.PRELOAD)) {
                createTransformTask(entity.getVideoGroupId(), entity.getId(), originPath, "videoServiceImpl.videoTransformPreloadCb", "videoServiceImpl.videoTransformFailCb");
            } else throw new BaseException(MessageConstant.ARG_ERROR);
        } else if (videoStatusWillBe != null) {
            videoMapper.updateById(entity.setStatus(videoStatusWillBe));
        }

        return entity.getId();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, key = "#addVideoDTO.videoGroupId"),
            @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, key = "#addVideoDTO.videoGroupId"),
    })
    @Transactional
    @Override
    public long addVideoWithCoverResolved(AddVideoDTO addVideoDTO, Byte videoStatusWillBe) {
        checkUserHaveTheGroup(addVideoDTO.getVideoGroupId());

        var entity = addVideoDTO.toEntity();
        videoMapper.insert(entity);

        // 链接处理
        if (addVideoDTO.getLink() != null && !addVideoDTO.getLink().isEmpty()) {
            var originPath = resourceLinkHandler.getRawPathFromTmpVideoLink(addVideoDTO.getLink());
            // 链接合法性检查 不是bv就正常检查
            if (!bvPattern.matcher(addVideoDTO.getLink()).find()) {
                if (!originPath.startsWith("tmp/user" + UserContext.getUserId() + "/"))
                    throw new BaseException(MessageConstant.INVALID_FILE_PATH);
            }
            if (bvPattern.matcher(addVideoDTO.getLink()).find()) {
                videoMapper.updateById(entity.setStatus(videoStatusWillBe));
                videoSrcMapper.insert(new VideoSrc().setVideoId(entity.getId()).setSrcName("720p").setSrc(addVideoDTO.getLink()));
            } else if (Objects.equals(videoStatusWillBe, Video.Status.ENABLE)) {
                createTransformTask(entity.getVideoGroupId(), entity.getId(), originPath, "videoServiceImpl.videoTransformEnableCb", "videoServiceImpl.videoTransformFailCb");
            } else if (Objects.equals(videoStatusWillBe, Video.Status.PRELOAD)) {
                createTransformTask(entity.getVideoGroupId(), entity.getId(), originPath, "videoServiceImpl.videoTransformPreloadCb", "videoServiceImpl.videoTransformFailCb");
            } else throw new BaseException(MessageConstant.ARG_ERROR);
        } else if (videoStatusWillBe != null) {
            videoMapper.updateById(entity.setStatus(videoStatusWillBe));
        }

        return entity.getId();
    }

    /**
     * 创建转码任务，并在超时后删除
     * @param videoId :
     * @param originPath 如 tmp/1/video.mp4
     */
    public void createTransformTask(Long videoGroupId, Long videoId, String originPath, String cbStr, String failCbStr) {
        // 视频转码
        var task = new VideoTransformTask()
                .setId(UUID.randomUUID() + "")
                .setVideoId(videoId)
                .setOriginPath(originPath)
                .setTargetPaths(new String[]{
                        "video-group/" + videoGroupId + "/" + videoId + "/360p.mp4",
                        "video-group/" + videoGroupId + "/" + videoId + "/720p.mp4",
                        "video-group/" + videoGroupId + "/" + videoId + "/1080p.mp4"
                })
                .setStatus(new VideoTransformTask.Status[]{VideoTransformTask.Status.WAITING, VideoTransformTask.Status.WAITING, VideoTransformTask.Status.WAITING})
                .setCbBeanNameAndMethodName(cbStr)
                .setFailCb(failCbStr);

        var plainUser = plainUserService.getPlainUserDetail(UserContext.getUserId());
        var name = plainUser == null ? "" : plainUser.getNickname();
        videoTransformer.transform(task, TRANSFORM_TASK_TTL, name);
    }

    @SuppressWarnings("unused")
    @Caching(evict = {
        @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, beforeInvocation = true, key = "#root.target.getVideoForce(#task.videoId) == null ? -1 : root.target.getVideoForce(#task.videoId).getVideoGroupId()"),
        @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, beforeInvocation = true, key = "#root.target.getVideoForce(#task.videoId) == null ? -1 : root.target.getVideoForce(#task.videoId).getVideoGroupId()"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_VO, key = "#task.videoId"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_SRC, key = "#task.videoId")
    })
    public void videoTransformFailCb(VideoTransformTask task) {
        videoMapper.deleteById(task.getVideoId());
    }

    @SuppressWarnings("unused")
    public void videoTransformEnableCb(VideoTransformTask task) {
        var self = SpringContextUtil.getBean(VideoServiceImpl.class);
        self.videoTransformSave(task, Video.Status.ENABLE);
    }

    @SuppressWarnings("unused")
    public void videoTransformPreloadCb(VideoTransformTask task) {
        var self = SpringContextUtil.getBean(VideoServiceImpl.class);
        self.videoTransformSave(task, Video.Status.PRELOAD);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConstant.VIDEO_VO, key = "#task.videoId"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_SRC, key = "#task.videoId"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, key = "#root.target.getVideoGroupIdFromVideoId(#task.videoId)"),
            @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, key = "#root.target.getVideoGroupIdFromVideoId(#task.videoId)")
    })
    @Transactional
    public void videoTransformSave(VideoTransformTask task, Byte videoStatus) {
        var videoInDB = videoMapper.selectById(task.getVideoId());
        if (videoInDB == null) {
            deleteVideo(task.getVideoId());
            return;
        }
        for (var taskType : task.getTaskTypes()) {
            if (videoSrcMapper.update(new LambdaUpdateWrapper<VideoSrc>()
                    .eq(VideoSrc::getVideoId, task.getVideoId())
                    .eq(VideoSrc::getSrcName, taskType.NAME)
                    .set(VideoSrc::getSrc, resourceLinkHandler.getVideoLink(task.getTargetPaths()[taskType.NUM]))) == 0
            ) {
                videoSrcMapper.insert(new VideoSrc()
                        .setVideoId(task.getVideoId())
                        .setSrcName(taskType.NAME)
                        .setSrc(resourceLinkHandler.getVideoLink(task.getTargetPaths()[taskType.NUM]))
                );
            }
        }
        if (Objects.equals(videoInDB.getStatus(), Video.Status.TRANSFORMING)) videoStatusUpdate(task.getVideoId(), videoStatus);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConstant.VIDEO_VO, key = "#videoId"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, key = "#root.target.getVideoGroupIdFromVideoId(#videoId)"),
            @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, key = "#root.target.getVideoGroupIdFromVideoId(#videoId)")
    })
    @Transactional
    public void videoStatusUpdate(Long videoId, Byte videoStatus) {
        videoMapper.updateById(new Video()
                .setId(videoId)
                .setStatus(videoStatus)
        );
        // 如果视频组显示正在转码(转码没设缓存)，那放出来
        if (Objects.equals(videoStatus, Video.Status.ENABLE)) {
            var videoGroup = videoGroupMapper.selectById(getVideoGroupIdFromVideoId(videoId));
            if (Objects.equals(videoGroup.getVideoGroupStatus(), VideoGroup.Status.TRANSFORMING)) {
                var videoGroupServiceBase = SpringContextUtil.getBean(VideoGroupServiceBase.class);
                videoGroupServiceBase.changeStatus(videoGroup.getId(), VideoGroup.Status.ENABLE);
            }
        }
    }

    @Override
    public boolean checkVideoPending(Long videoId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConstant.VIDEO_TRANSFORM_TASK_VIDEO_ID + videoId));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, key = "#updateVideoDTO.videoGroupId == null ? -1 : #updateVideoDTO.videoGroupId"),
            @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, key = "#updateVideoDTO.videoGroupId == null ? -1 : #updateVideoDTO.videoGroupId"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, key = "#root.target.getVideoGroupIdFromVideoId(#updateVideoDTO.id)"),
            @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, key = "#root.target.getVideoGroupIdFromVideoId(#updateVideoDTO.id)"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_VO, key = "#updateVideoDTO.id"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_SRC, key = "#updateVideoDTO.id")
    })
    @Transactional
    @Override
    public void updateVideo(UpdateVideoDTO updateVideoDTO, Byte videoStatusWillBe) {
        checkUserHaveTheGroup(getVideoGroupIdFromVideoId(updateVideoDTO.getId()));
        if (updateVideoDTO.getVideoGroupId() != null) {
            checkUserHaveTheGroup(updateVideoDTO.getVideoGroupId());
        } else {
            updateVideoDTO.setVideoGroupId(getVideoGroupIdFromVideoId(updateVideoDTO.getId()));
        }

        var entity = updateVideoDTO.toEntity();

        if (updateVideoDTO.getLink() != null) {
            var originPath = resourceLinkHandler.getRawPathFromTmpVideoLink(updateVideoDTO.getLink());
            // 链接合法性检查 不是bv就正常检查
            if (!bvPattern.matcher(updateVideoDTO.getLink()).find()) {
                if (!originPath.startsWith("tmp/user" + UserContext.getUserId() + "/"))
                    throw new BaseException(MessageConstant.INVALID_FILE_PATH);
            }
            if (Objects.equals(videoMapper.selectById(updateVideoDTO.getId()).getStatus(), Video.Status.TRANSFORMING))
                throw new BaseException(MessageConstant.VIDEO_TRANSFORMING);

            // 链接处理
            videoSrcMapper.delete(new LambdaQueryWrapper<VideoSrc>()
                    .eq(VideoSrc::getVideoId, updateVideoDTO.getId())
            );
            if (bvPattern.matcher(updateVideoDTO.getLink()).find()) {
                videoMapper.updateById(entity.setStatus(videoStatusWillBe));
                videoSrcMapper.insert(new VideoSrc().setVideoId(entity.getId()).setSrcName("720p").setSrc(updateVideoDTO.getLink()));
            } else if (Objects.equals(videoStatusWillBe, Video.Status.ENABLE)) {
                createTransformTask(updateVideoDTO.getVideoGroupId(), updateVideoDTO.getId(), originPath, "videoServiceImpl.videoTransformEnableCb", "videoServiceImpl.videoTransformFailCb");
            } else if (Objects.equals(videoStatusWillBe, Video.Status.PRELOAD)) {
                createTransformTask(updateVideoDTO.getVideoGroupId(), updateVideoDTO.getId(), originPath, "videoServiceImpl.videoTransformPreloadCb", "videoServiceImpl.videoTransformFailCb");
            } else throw new BaseException(MessageConstant.ARG_ERROR);
        } else if (videoStatusWillBe != null) {
            videoMapper.updateById(entity.setStatus(videoStatusWillBe));
        }

        var coverUrl = updateVideoDTO.getCover();
        if (coverUrl != null) {
            try {
                var cover = fileService.changeTmpFileToStatic(
                        coverUrl,
                        "/video-group/" + updateVideoDTO.getVideoGroupId() + "/" + updateVideoDTO.getId(),
                        "cover" + coverUrl.substring(coverUrl.lastIndexOf('.'))
                );
                if (cover.isEmpty()) throw new Exception();
                entity.setCover(cover);
            } catch (Exception e) {
                throw new BaseException(MessageConstant.INVALID_FILE_PATH);
            }
        }

        videoMapper.updateById(entity);
    }

    @Async("slowExecutor")
    @Caching(evict = {
        @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, beforeInvocation = true, key = "#root.target.getVideoForce(#videoId) == null ? -1 :#root.target.getVideoForce(#videoId).getVideoGroupId()"),
        @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, beforeInvocation = true, key = "#root.target.getVideoForce(#videoId) == null ? -1 :#root.target.getVideoForce(#videoId).getVideoGroupId()"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_VO, key = "#videoId"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_SRC, key = "#videoId")
    })
    @Override
    public void deleteVideo(Long videoId) {
        var obj = videoMapper.selectById(videoId);
        if (obj == null) return;
        // 检查是不是拥有者
        checkUserHaveTheGroup(obj.getVideoGroupId());
        // 检查是否正在转码
//        if (obj.getStatus().equals(Video.Status.TRANSFORMING)) throw new BaseException(MessageConstant.VIDEO_TRANSFORMING);

        videoMapper.deleteById(videoId); // 视频源有外键，不用手动处理
        fileService.deleteDirInSystem("/video-group/" + obj.getVideoGroupId() + "/" + videoId);
        // 删除弹幕
        danmakuMapper.delete(new LambdaQueryWrapper<Danmaku>().eq(Danmaku::getVideoId, videoId));
    }

    @Override
    public VideoVO getVideo(Long videoId) {
        var self = SpringContextUtil.getBean(getClass());
        var vo = self.getVideoBase(videoId);
        if (vo == null) return null;
        self.setVideoSrcTo(vo);
        return vo;
    }

    @Override
    public VideoForceVO getVideoForce(Long videoId) {
        var video = videoMapper.selectById(videoId);
        if (video == null) return null;

        var vo = new VideoForceVO();
        BeanUtils.copyProperties(video, vo);

        var self = SpringContextUtil.getBean(getClass());
        self.setVideoSrcTo(vo);
        return vo;
    }

    @Cacheable(cacheNames = RedisConstant.VIDEO_VO, key = "#videoId", unless = "#result == null")
    public VideoVO getVideoBase(Long videoId) {
        var video = videoMapper.selectById(videoId);
        if (video == null) return null;
        if (!video.getStatus().equals(Video.Status.ENABLE)) return null;

        var vo = new VideoVO();
        BeanUtils.copyProperties(video, vo);
        return vo;
    }

    public void setVideoSrcTo(VideoVO vo) {
        if (Objects.equals(vo.getStatus(), Video.Status.TRANSFORMING)) {
            vo.setSrc(new ArrayList<>(List.of(new VideoSrcVO("1080p", moeProperties.getDefaultVideoPath()))));
        } else {
            var self = SpringContextUtil.getBean(getClass());
            vo.setSrc(self.getVideoSrcFromDB(vo.getId()));
            parseBV(vo);
        }
    }

    @Cacheable(cacheNames = RedisConstant.VIDEO_SRC, key = "#videoId", unless = "#result.isEmpty()")
    public ArrayList<VideoSrcVO> getVideoSrcFromDB(Long videoId) {
        return new ArrayList<>(
                videoSrcMapper.selectList(new LambdaQueryWrapper<VideoSrc>()
                        .select(VideoSrc::getSrcName, VideoSrc::getSrc)
                        .eq(VideoSrc::getVideoId, videoId)
                ).stream().map(videoSrc -> new VideoSrcVO(videoSrc.getSrcName(), videoSrc.getSrc())).toList()
        );
    }

    /**
     * 解析bv号 设置到src对象里面
     */
    protected void parseBV(VideoVO vo) {
        if (!vo.getSrc().isEmpty() && vo.getSrc().getFirst().getSrc().startsWith("BV")) {
            // 设置bvid
            var first = vo.getSrc().getFirst();
            vo.setBvid(first.getSrc());
            // 设置src
            var taskList = new ArrayList<CompletableFuture<String>>(vo.getSrc().size());
            for (var srcObj : vo.getSrc()) {
                if (srcObj.getSrc().startsWith("BV")) {
                    taskList.add(CompletableFuture.supplyAsync(() -> {
                        String url = "";
                        try {
                            url = biliParser.parseBV(srcObj.getSrc(), srcObj.getSrcName(), vo.getIndex()+"");
                        } catch (Exception ignored) {}
                        try {
                            if (url.isEmpty()) url = biliParser.parseBV(srcObj.getSrc(), srcObj.getSrcName(), "1");
                        } catch (Exception ignored) {}
                        return url;
                    }, Executors.newVirtualThreadPerTaskExecutor()));
                }
            }
            try {
                CompletableFuture.allOf(taskList.toArray(CompletableFuture[]::new)).get();
            } catch (Exception e) {
                throw new BaseException(MessageConstant.INVALID_FILE_PATH);
            }
            for (int i = 0; i < taskList.size(); i++) {
                vo.getSrc().get(i).setSrc(taskList.get(i).join());
            }
        }
    }

    @SuppressWarnings("unused")
    public Long getVideoGroupIdFromVideoId(Long id) {
        var video = videoMapper.selectById(id);
        if (video == null) return -1L;
        return video.getVideoGroupId();
    }

    @Transactional
    @Override
    public void updateManyIndex(List<UpdateManyVideoIndexDTO.UpdateVideoIndexDTO> arr) {
        for (var dto : arr) {
            videoMapper.update(new LambdaUpdateWrapper<Video>()
                    .eq(Video::getId, dto.getVideoId())
                    .set(Video::getIndex, dto.getIndex())
            );
        }
    }

    public void checkUserHaveTheGroup(Long videoGroupId) {
        var videoGroupService = SpringContextUtil.getBean(VideoGroupServiceBase.class);
        videoGroupService.checkUserHaveTheGroup(videoGroupId);
    }
}