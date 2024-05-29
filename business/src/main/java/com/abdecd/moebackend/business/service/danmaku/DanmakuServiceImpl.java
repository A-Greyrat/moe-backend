package com.abdecd.moebackend.business.service.danmaku;

import com.abdecd.moebackend.business.dao.entity.Danmaku;
import com.abdecd.moebackend.business.dao.mapper.DanmakuMapper;
import com.abdecd.moebackend.business.lib.CacheByFrequency;
import com.abdecd.moebackend.business.lib.CacheByFrequencyFactory;
import com.abdecd.moebackend.business.lib.RedisHelper;
import com.abdecd.moebackend.business.pojo.dto.danmaku.AddDanmakuDTO;
import com.abdecd.moebackend.business.pojo.vo.danmaku.DanmakuVO;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.tokenlogin.common.context.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class DanmakuServiceImpl implements DanmakuService {
    @Autowired
    private DanmakuMapper danmakuMapper;
    @Autowired
    private RedisTemplate<String, LocalDateTime> redisTemplate;
    @Autowired
    private RedisHelper redisHelper;
    private CacheByFrequency<List<DanmakuVO>> danmakuCache;
    @Autowired
    public void setDanmakuCache(CacheByFrequencyFactory cacheByFrequencyFactory) {
        danmakuCache = cacheByFrequencyFactory.create(RedisConstant.DANMAKU, 200, 15);
    }

    @CacheEvict(value = RedisConstant.VIDEO_DANMAKU_CNT, key = "#addDanmakuDTO.videoId")
    @Override
    public Long addDanmaku(AddDanmakuDTO addDanmakuDTO) {
        var entity = addDanmakuDTO.toEntity(UserContext.getUserId());
        danmakuMapper.insert(entity);
        danmakuCache.deleteMany(entity.getVideoId() + ":*");
        updateDanmakuTimestamp(entity.getVideoId());
        return entity.getId();
    }

    @Override
    public List<DanmakuVO> getDanmaku(Long videoId, Integer segmentIndex) {
        danmakuCache.recordFrequency(videoId + ":" + segmentIndex);
        return danmakuCache.get(videoId + ":" + segmentIndex, () -> {
            var list = danmakuMapper.selectList(new LambdaQueryWrapper<Danmaku>()
                    .eq(Danmaku::getVideoId, videoId)
                    .ge(Danmaku::getBegin, (segmentIndex - 1) * 360)
                    .lt(Danmaku::getBegin, segmentIndex * 360)
                    .orderByDesc(Danmaku::getTime)
                    .last("limit 5000")
            );
            return new ArrayList<>(list.stream().map(x -> {
                var danmakuVO = new DanmakuVO();
                BeanUtils.copyProperties(x, danmakuVO);
                return danmakuVO;
            }).toList());
        }, null, null);
    }

    @CacheEvict(value = RedisConstant.VIDEO_DANMAKU_CNT, beforeInvocation = true, key = "#root.target.getVideoIdFromDanmaku(#id)")
    @Override
    public void deleteDanmaku(Long id) {
        var danmaku = danmakuMapper.selectById(id);
        if (danmaku == null) return;
        if (
                danmaku.getUserId().equals(UserContext.getUserId())
                && danmaku.getTimestamp().isAfter(LocalDateTime.now().minusMinutes(2))
        ) {
            danmakuMapper.deleteById(id);
        }
        danmakuCache.deleteMany(danmaku.getVideoId() + ":*");
        updateDanmakuTimestamp(danmaku.getVideoId());
    }

    @SuppressWarnings("unused")
    public Long getVideoIdFromDanmaku(long id) {
        var danmaku = danmakuMapper.selectById(id);
        if (danmaku == null) return -1L;
        return danmaku.getVideoId();
    }

    @Cacheable(value = RedisConstant.VIDEO_DANMAKU_CNT, key = "#videoId")
    @Override
    public Long getDanmakuCount(Long videoId) {
        return danmakuMapper.selectCount(new LambdaQueryWrapper<Danmaku>()
                .eq(Danmaku::getVideoId, videoId)
        );
    }

    @Override
    public LocalDateTime getDanmakuTimestamp(Long videoId, Integer segmentIndex) {
        if (getDanmaku(videoId, segmentIndex).isEmpty()) return LocalDateTime.now();
        var key = RedisConstant.TIMESTAMP_DANMAKU + videoId + ":" + segmentIndex;
        redisTemplate.opsForValue().setIfAbsent(key, LocalDateTime.now());
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void updateDanmakuTimestamp(Long videoId) {
        var keys = redisHelper.scan(RedisConstant.TIMESTAMP_DANMAKU + videoId + ":*");
        if (keys.isEmpty()) return;
        var newTime = LocalDateTime.now();
        var map = new HashMap<String, LocalDateTime>();
        for (var key : keys) map.put(key, newTime);
        redisTemplate.opsForValue().multiSet(map);
    }
}
