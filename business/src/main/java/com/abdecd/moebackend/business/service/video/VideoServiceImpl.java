package com.abdecd.moebackend.business.service.video;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.common.property.MoeProperties;
import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.entity.VideoSrc;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoSrcMapper;
import com.abdecd.moebackend.business.lib.BiliParser;
import com.abdecd.moebackend.business.lib.ResourceLinkHandler;
import com.abdecd.moebackend.business.pojo.dto.video.AddVideoDTO;
import com.abdecd.moebackend.business.pojo.dto.video.UpdateVideoDTO;
import com.abdecd.moebackend.business.pojo.dto.video.VideoTransformTask;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private RedisTemplate<String, VideoTransformTask> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ResourceLinkHandler resourceLinkHandler;
    @Autowired
    private FileService fileService;
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    @Autowired
    private BiliParser biliParser;
    @Autowired
    private PlainUserService plainUserService;
    @Autowired
    private MoeProperties moeProperties;
    @Autowired
    private VideoGroupMapper videoGroupMapper;

    private static final int TRANSFORM_TASK_TTL = 600;
    private static final int TRANSFORM_TASK_REDIS_TTL = 1300;

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, key = "#addVideoDTO.videoGroupId"),
            @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, key = "#addVideoDTO.videoGroupId"),
    })
    @Transactional
    @Override
    public long addVideo(AddVideoDTO addVideoDTO) {
        checkUserHaveTheGroup(addVideoDTO.getVideoGroupId());

        var originPath = resourceLinkHandler.getRawPathFromTmpVideoLink(addVideoDTO.getLink());
        if (!originPath.startsWith("tmp/user" + UserContext.getUserId() + "/"))
            throw new BaseException(MessageConstant.INVALID_FILE_PATH);

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
        createTransformTask(entity.getVideoGroupId(), entity.getId(), originPath, "videoServiceImpl.videoTransformCbWillFinish");

        return entity.getId();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, key = "#addVideoDTO.videoGroupId"),
            @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, key = "#addVideoDTO.videoGroupId"),
    })
    @Transactional
    @Override
    public long addVideoWithCoverResolved(AddVideoDTO addVideoDTO) {
        checkUserHaveTheGroup(addVideoDTO.getVideoGroupId());

        var originPath = resourceLinkHandler.getRawPathFromTmpVideoLink(addVideoDTO.getLink());
        if (!originPath.startsWith("tmp/user" + UserContext.getUserId() + "/"))
            throw new BaseException(MessageConstant.INVALID_FILE_PATH);

        var entity = addVideoDTO.toEntity();
        videoMapper.insert(entity);

        // 链接处理
        createTransformTask(entity.getVideoGroupId(), entity.getId(), originPath, "videoServiceImpl.videoTransformCbWillFinish");

        return entity.getId();
    }

    /**
     * 创建转码任务，并在超时后删除
     * @param videoId :
     * @param originPath 如 tmp/1/video.mp4
     */
    private void createTransformTask(Long videoGroupId, Long videoId, String originPath, String cbStr) {
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
                .setCbBeanNameAndMethodName(cbStr);
        // 保存任务
        redisTemplate.opsForValue().set(RedisConstant.VIDEO_TRANSFORM_TASK_PREFIX + task.getId(), task, TRANSFORM_TASK_REDIS_TTL, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(RedisConstant.VIDEO_TRANSFORM_TASK_VIDEO_ID + task.getVideoId(), task.getId(), TRANSFORM_TASK_REDIS_TTL, TimeUnit.SECONDS);

        var plainUser = plainUserService.getPlainUserDetail(UserContext.getUserId());
        var name = plainUser == null ? "" : plainUser.getNickname();
        videoTransformer.transform(task, TRANSFORM_TASK_TTL + 10, name);
        // todo 用消息队列 超时去删数据库
        var taskId = task.getId();
        scheduledExecutor.schedule(() -> {
            var nowTask = redisTemplate.opsForValue().get(RedisConstant.VIDEO_TRANSFORM_TASK_PREFIX + taskId);
            if (nowTask != null) {
                redisTemplate.delete(RedisConstant.VIDEO_TRANSFORM_TASK_PREFIX + nowTask.getId());
                stringRedisTemplate.delete(RedisConstant.VIDEO_TRANSFORM_TASK_VIDEO_ID + nowTask.getVideoId());
                videoMapper.deleteById(nowTask.getVideoId());
            }
        }, TRANSFORM_TASK_TTL, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unused")
    public void videoTransformCbWillFinish(VideoTransformTask task) {
        redisTemplate.delete(RedisConstant.VIDEO_TRANSFORM_TASK_PREFIX + task.getId());
        stringRedisTemplate.delete(RedisConstant.VIDEO_TRANSFORM_TASK_VIDEO_ID + task.getVideoId());
        var self = SpringContextUtil.getBean(VideoServiceImpl.class);
        self.videoTransformSave(task, Video.Status.ENABLE);
    }

    @CacheEvict(cacheNames = RedisConstant.VIDEO_VO, key = "#task.videoId")
    @Transactional
    public void videoTransformSave(VideoTransformTask task, Byte videoStatus) {
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
        videoMapper.updateById(new Video()
                .setId(task.getVideoId())
                .setStatus(videoStatus)
        );
        // 如果视频组显示正在转码(转码没设缓存)，那放出来
        if (Objects.equals(videoStatus, Video.Status.ENABLE)) {
            var self = SpringContextUtil.getBean(VideoServiceImpl.class);
            var videoGroup = videoGroupMapper.selectById(self.getVideo(task.getVideoId()).getVideoGroupId());
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
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, key = "#updateVideoDTO.videoGroupId"),
            @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, key = "#updateVideoDTO.videoGroupId"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_VO, key = "#updateVideoDTO.id")
    })
    @Override
    public void updateVideo(UpdateVideoDTO updateVideoDTO) {
        checkUserHaveTheGroup(updateVideoDTO.getVideoGroupId());

        if (updateVideoDTO.getLink() != null) {
            var originPath = resourceLinkHandler.getRawPathFromTmpVideoLink(updateVideoDTO.getLink());
            if (!originPath.startsWith("tmp/user" + UserContext.getUserId() + "/"))
                throw new BaseException(MessageConstant.INVALID_FILE_PATH);
            if (Objects.equals(videoMapper.selectById(updateVideoDTO.getId()).getStatus(), Video.Status.TRANSFORMING))
                throw new BaseException(MessageConstant.VIDEO_TRANSFORMING);
            createTransformTask(updateVideoDTO.getVideoGroupId(), updateVideoDTO.getId(), originPath, "videoServiceImpl.videoTransformCbWillFinish");
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
                updateVideoDTO.setCover(cover);
            } catch (Exception e) {
                throw new BaseException(MessageConstant.INVALID_FILE_PATH);
            }
        }

        var entity = updateVideoDTO.toEntity();
        videoMapper.updateById(entity);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConstant.VIDEO_GROUP_CONTENTS_CACHE, beforeInvocation = true, key = "#root.target.getVideo(#videoId).getVideoGroupId()"),
            @CacheEvict(cacheNames = RedisConstant.BANGUMI_VIDEO_GROUP_CONTENTS_CACHE, beforeInvocation = true, key = "#root.target.getVideo(#videoId).getVideoGroupId()"),
            @CacheEvict(cacheNames = RedisConstant.VIDEO_VO, key = "#videoId")
    })
    @Override
    public void deleteVideo(Long videoId) {
        var obj = videoMapper.selectById(videoId);
        if (obj == null) return;
        // 检查是不是拥有者
        checkUserHaveTheGroup(obj.getVideoGroupId());
        // 检查是否正在转码
        if (obj.getStatus().equals(Video.Status.TRANSFORMING)) throw new BaseException(MessageConstant.VIDEO_TRANSFORMING);

        videoMapper.deleteById(videoId); // 视频源有外键，不用手动处理
        fileService.deleteDirInSystem("/video-group/" + obj.getVideoGroupId() + "/" + videoId);
    }

    @Cacheable(cacheNames = RedisConstant.VIDEO_VO, key = "#videoId", unless = "#result == null")
    @Override
    public VideoVO getVideo(Long videoId) {
        var video = videoMapper.selectById(videoId);
        if (video == null) return null;

        var vo = new VideoVO();
        BeanUtils.copyProperties(video, vo);
        // 在转码中的视频不需要返回
        if (Objects.equals(video.getStatus(), Video.Status.TRANSFORMING)) {
            vo.setSrc(new ArrayList<>(List.of(new VideoSrcVO("1080p", moeProperties.getDefaultVideoPath()))));
        } else {
            vo.setSrc(new ArrayList<>(
                    videoSrcMapper.selectList(new LambdaQueryWrapper<VideoSrc>()
                                    .eq(VideoSrc::getVideoId, videoId)
                                    .select(VideoSrc::getSrcName, VideoSrc::getSrc))
                            .stream().map(videoSrc -> new VideoSrcVO(videoSrc.getSrcName(), videoSrc.getSrc())).toList()
            ));
            parseBV(vo);
        }

        return vo;
    }

    /**
     * 解析bv号 设置到src对象里面
     */
    private void parseBV(VideoVO vo) {
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

    @Override
    @Cacheable(cacheNames = RedisConstant.VIDEO_LIST_CACHE,key = "#videoGroupId") // todo
    public ArrayList<Video> getVideoListByGid(Long videoGroupId) {
        return videoMapper.selectByGid(videoGroupId);
    }


    public void checkUserHaveTheGroup(Long videoGroupId) {
        var videoGroupService = SpringContextUtil.getBean(VideoGroupServiceBase.class);
        videoGroupService.checkUserHaveTheGroup(videoGroupId);
    }
}