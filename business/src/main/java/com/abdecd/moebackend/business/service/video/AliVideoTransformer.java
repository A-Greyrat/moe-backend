package com.abdecd.moebackend.business.service.video;

import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.abdecd.moebackend.business.lib.AliImmManager;
import com.abdecd.moebackend.business.pojo.dto.video.VideoTransformCbArgs;
import com.abdecd.moebackend.business.pojo.dto.video.VideoTransformTask;
import com.abdecd.moebackend.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AliVideoTransformer implements VideoTransformer {
    @Autowired
    private RedisTemplate<String, VideoTransformTask> redisTemplate;
    @Autowired
    private AliImmManager aliImmManager;
    @Autowired
    private RedissonClient redissonClient;

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public void transform(VideoTransformTask task, int ttlSeconds, String username) {
        // 访问视频转码服务
        transform(username, task.getId(), VideoTransformTask.TaskType.VIDEO_TRANSFORM_360P, task.getOriginPath(), task.getTargetPaths()[VideoTransformTask.TaskType.VIDEO_TRANSFORM_360P.NUM], "640x360", ttlSeconds);
        transform(username, task.getId(), VideoTransformTask.TaskType.VIDEO_TRANSFORM_720P, task.getOriginPath(), task.getTargetPaths()[VideoTransformTask.TaskType.VIDEO_TRANSFORM_720P.NUM], "1280x720", ttlSeconds);
        transform(username, task.getId(), VideoTransformTask.TaskType.VIDEO_TRANSFORM_1080P, task.getOriginPath(), task.getTargetPaths()[VideoTransformTask.TaskType.VIDEO_TRANSFORM_1080P.NUM], "1920x1080", ttlSeconds);
    }
    public void transform(
            String username,
            String taskId,
            VideoTransformTask.TaskType taskType,
            String originPath,
            String targetPath,
            String widthAndHeight,
            int ttlSeconds
    ) {
        // 访问视频转码服务
        var aliTaskId = aliImmManager.transformVideo(username, originPath, targetPath, widthAndHeight);
        log.info("aliTaskId: " + aliTaskId);
        // 轮询转码结果
        int perAskTtl = ttlSeconds / 10;
        Runnable cb = new Runnable() {
            int count = 10;
            @Override
            public void run() {
                var result = aliImmManager.getTransformResult(aliTaskId);
                log.info("aliTaskId: " + aliTaskId + ", result: " + result);
                if (result.equals("Succeeded")) {
                    transformCb(taskId, taskType);
                    return;
                } else if (result.equals("Failed")) {
                    return;
                }
                count--;
                if (count > 0) scheduledExecutor.schedule(this, perAskTtl, TimeUnit.SECONDS);
            }
        };
        scheduledExecutor.schedule(cb, perAskTtl, TimeUnit.SECONDS);
    }

    public void transformCb(String taskId, VideoTransformTask.TaskType taskType) {
        videoTransformCb(new VideoTransformCbArgs(taskId, taskType, VideoTransformCbArgs.Status.SUCCESS));
    }

    public void videoTransformCb(VideoTransformCbArgs cbArgs) {
        var lock = redissonClient.getLock(RedisConstant.VIDEO_TRANSFORM_TASK_CB_LOCK + cbArgs.getTaskId());
        lock.lock();
        log.info("videoTransformCb:" + cbArgs);
        try {
            if (cbArgs.getStatus().equals(VideoTransformCbArgs.Status.SUCCESS)) {
                var task = redisTemplate.opsForValue().get(RedisConstant.VIDEO_TRANSFORM_TASK_PREFIX + cbArgs.getTaskId());
                if (task != null) {
                    // 更改状态并保存
                    task.getStatus()[cbArgs.getType().NUM] = VideoTransformTask.Status.SUCCESS;
                    redisTemplate.opsForValue().set(RedisConstant.VIDEO_TRANSFORM_TASK_PREFIX + cbArgs.getTaskId(), task);
                    // 检验状态并调用结束任务
                    for (var status : task.getStatus()) {
                        if (status == VideoTransformTask.Status.WAITING) return;
                    }
                    videoTransformCbWillFinish(task);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void videoTransformCbWillFinish(VideoTransformTask task) {
        // 使用反射触发回调
        var strs = task.getCbBeanNameAndMethodName().split("\\.");
        var bean = SpringContextUtil.getBean(strs[0]);
        try {
            Method method = bean.getClass().getDeclaredMethod(strs[1], VideoTransformTask.class);
            method.invoke(bean, task);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
   }
}
