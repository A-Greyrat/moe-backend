package com.abdecd.moebackend.business.service.plainuser;

import com.abdecd.moebackend.business.dao.entity.PlainUserHistory;
import com.abdecd.moebackend.business.pojo.dto.plainuser.AddHistoryDTO;
import com.abdecd.moebackend.business.pojo.vo.plainuser.HistoryVO;
import com.abdecd.moebackend.business.pojo.vo.plainuser.UploaderVO;
import com.abdecd.moebackend.business.pojo.vo.video.VideoVO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupVO;
import com.abdecd.moebackend.business.service.video.VideoService;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.result.PageVO;
import com.abdecd.tokenlogin.common.context.UserContext;
import jakarta.annotation.Nullable;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class PlainUserHistoryService {
    @Autowired
    private RedisTemplate<String, PlainUserHistory> redisTemplate;
    @Autowired
    private VideoService videoService;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private PlainUserHistoryBatchSaver plainUserHistoryBatchSaver;
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;

    public PageVO<HistoryVO> getHistory(Integer page, Integer pageSize) {
        var list = redisTemplate.opsForList().range(
                RedisConstant.PLAIN_USER_HISTORY + UserContext.getUserId(),
                (long) (page - 1) * pageSize,
                (long) page * pageSize - 1
        );
        if (list == null) return new PageVO<>();
        var total = redisTemplate.opsForList().size(RedisConstant.PLAIN_USER_HISTORY + UserContext.getUserId());
        if (total == null) return new PageVO<>();
        return new PageVO<>(Math.toIntExact(total), list.stream().map(this::formHistoryVO).toList());
    }

    /**
     * 获取历史记录
     * @param index index
     * @param pageSize 数量
     */
    public PageVO<HistoryVO> getHistory2(Integer index, Integer pageSize) {
        var list = redisTemplate.opsForList().range(
                RedisConstant.PLAIN_USER_HISTORY + UserContext.getUserId(),
                Math.max(0, index),
                (long) index + pageSize - 1
        );
        if (list == null) return new PageVO<>();
        var total = redisTemplate.opsForList().size(RedisConstant.PLAIN_USER_HISTORY + UserContext.getUserId());
        if (total == null) return new PageVO<>();
        return new PageVO<>(Math.toIntExact(total), list.stream().map(this::formHistoryVO).toList());
    }

    public HistoryVO formHistoryVO(PlainUserHistory plainUserHistory) {
        var video = videoService.getVideoBase(plainUserHistory.getVideoId());
        if (video == null) video = new VideoVO();
        var videoGroup = videoGroupServiceBase.getVideoGroupInfo(plainUserHistory.getVideoGroupId());
        if (videoGroup == null) videoGroup = new VideoGroupVO();
        var uploaderVO = videoGroup.getUploader();
        if (uploaderVO == null) uploaderVO = new UploaderVO();

        return new HistoryVO()
                .setUploader(uploaderVO)
                .setVideoGroupId(videoGroup.getId())
                .setVideoGroupType(videoGroup.getType())
                .setVideoGroupTitle(videoGroup.getTitle())
                .setVideoGroupCover(videoGroup.getCover())
                .setVideoId(video.getId())
                .setVideoIndex(video.getIndex())
                .setVideoTitle(video.getTitle())
                .setTimestamp(plainUserHistory.getTimestamp());
    }

    @Nullable
    public HistoryVO getLatestHistory(long videoGroupId) {
        var list = redisTemplate.opsForList().range(RedisConstant.PLAIN_USER_HISTORY + UserContext.getUserId(), 0, -1);
        if (list == null) return null;
        for (var item : list) {
            if (item.getVideoGroupId().equals(videoGroupId)) {
                return formHistoryVO(item);
            }
        }
        return null;
    }

    public void addHistory(AddHistoryDTO addHistoryDTO) {
        var video = videoService.getVideoBase(addHistoryDTO.getVideoId());
        if (video == null) return;
        var videoGroup = videoGroupServiceBase.getVideoGroupInfo(video.getVideoGroupId());
        if (videoGroup == null) return;
        var entity = addHistoryDTO.toEntity(videoGroup.getId());

        // add to redis
        var lock = redissonClient.getLock(RedisConstant.PLAIN_USER_HISTORY_LOCK + addHistoryDTO.getUserId());
        lock.lock();
        try {
            // delete old record
            var list = redisTemplate.opsForList().range(RedisConstant.PLAIN_USER_HISTORY + addHistoryDTO.getUserId(), 0, -1);
            if (list == null) list = new ArrayList<>();
            for (var item : list) {
                if (item.getVideoGroupId().equals(entity.getVideoGroupId())) {
                    redisTemplate.opsForList().remove(RedisConstant.PLAIN_USER_HISTORY + addHistoryDTO.getUserId(), 0, item);
                }
            }
            // add new record
            redisTemplate.opsForList().leftPush(RedisConstant.PLAIN_USER_HISTORY + addHistoryDTO.getUserId(), entity);
            redisTemplate.opsForList().trim(
                    RedisConstant.PLAIN_USER_HISTORY + addHistoryDTO.getUserId(),
                    0,
                    RedisConstant.PLAIN_USER_HISTORY_SIZE
            );
        } finally {
            lock.unlock();
        }
        // add to db
        plainUserHistoryBatchSaver.add(entity);
    }

    public void deleteHistory(long[] videoGroupIds) {
        // delete from redis
        var lock = redissonClient.getLock(RedisConstant.PLAIN_USER_HISTORY_LOCK + UserContext.getUserId());
        lock.lock();
        try {
            // delete record
            var list = redisTemplate.opsForList().range(RedisConstant.PLAIN_USER_HISTORY + UserContext.getUserId(), 0, -1);
            if (list == null) list = new ArrayList<>();
            for (var item : list) {
                for (var id : videoGroupIds) {
                    if (item.getVideoGroupId().equals(id)) {
                        redisTemplate.opsForList().remove(RedisConstant.PLAIN_USER_HISTORY + UserContext.getUserId(), 0, item);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
