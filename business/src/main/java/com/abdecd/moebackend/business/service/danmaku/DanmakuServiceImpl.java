package com.abdecd.moebackend.business.service.danmaku;

import com.abdecd.moebackend.business.common.util.SpringContextUtil;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
public class DanmakuServiceImpl implements DanmakuService {
    @Autowired
    private DanmakuMapper danmakuMapper;
    @Autowired
    private RedisTemplate<String, LocalDateTime> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisHelper redisHelper;
    private CacheByFrequency<List<DanmakuVO>> danmakuCache;

    @Autowired
    public void setDanmakuCache(CacheByFrequencyFactory cacheByFrequencyFactory) {
        danmakuCache = cacheByFrequencyFactory.create(RedisConstant.DANMAKU, 200, 15);
    }

    @Override
    public Long addDanmaku(AddDanmakuDTO addDanmakuDTO) {
        var entity = addDanmakuDTO.toEntity(UserContext.getUserId());
        danmakuMapper.insert(entity);
        danmakuCache.deleteMany(entity.getVideoId() + ":*");
        updateDanmakuTimestamp(entity.getVideoId());
        // 更新弹幕数量
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(RedisConstant.VIDEO_DANMAKU_CNT + "::" + entity.getVideoId()))) {
            var self = SpringContextUtil.getBean(getClass());
            self.getDanmakuCount(entity.getVideoId());
        }
        stringRedisTemplate.opsForValue().increment(RedisConstant.VIDEO_DANMAKU_CNT + "::" + entity.getVideoId());
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

    @Override
    public void deleteDanmaku(Long id) {
        var danmaku = danmakuMapper.selectById(id);
        if (danmaku == null) return;
        if (
            danmaku.getUserId().equals(UserContext.getUserId())
                && danmaku.getTime() >= (new Date().getTime() - 1000 * 120)
        ) {
            danmakuMapper.deleteById(id);
            danmakuCache.deleteMany(danmaku.getVideoId() + ":*");
            updateDanmakuTimestamp(danmaku.getVideoId());
            // 更新弹幕数量
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(RedisConstant.VIDEO_DANMAKU_CNT + "::" + danmaku.getVideoId()))) {
                var self = SpringContextUtil.getBean(getClass());
                self.getDanmakuCount(danmaku.getVideoId());
            }
            stringRedisTemplate.opsForValue().decrement(RedisConstant.VIDEO_DANMAKU_CNT + "::" + danmaku.getVideoId());
        }
    }

    @Override
    public Long getDanmakuCount(Long videoId) {
        var self = SpringContextUtil.getBean(getClass());
        return Long.parseLong(self.getDanmakuCountCache(videoId));
    }

    //    @Cacheable(value = RedisConstant.VIDEO_DANMAKU_CNT, key = "#videoId")
    public String getDanmakuCountCache(Long videoId) {
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(RedisConstant.VIDEO_DANMAKU_CNT + "::" + videoId))) {
            var value = danmakuMapper.selectCount(new LambdaQueryWrapper<Danmaku>()
                .eq(Danmaku::getVideoId, videoId)
            ) + "";
            stringRedisTemplate.opsForValue().set(RedisConstant.VIDEO_DANMAKU_CNT + "::" + videoId, value);
        }
        return stringRedisTemplate.opsForValue().get(RedisConstant.VIDEO_DANMAKU_CNT + "::" + videoId);
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
