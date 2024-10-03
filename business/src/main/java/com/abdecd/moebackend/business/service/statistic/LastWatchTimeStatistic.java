package com.abdecd.moebackend.business.service.statistic;

import com.abdecd.moebackend.business.dao.entity.PlainUserLastWatchTime;
import com.abdecd.moebackend.business.dao.mapper.PlainUserLastWatchTimeMapper;
import com.abdecd.moebackend.business.lib.RedisAsyncSetter;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LastWatchTimeStatistic {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PlainUserLastWatchTimeMapper plainUserLastWatchTimeMapper;
    @Autowired
    private RedisAsyncSetter<String> redisAsyncSetter;

    public void add(Long videoId, Long watchProgress) {
        redisAsyncSetter.set(
                stringRedisTemplate,
                RedisConstant.PLAIN_USER_LAST_WATCH_TIME + UserContext.getUserId() + ":" + videoId,
                watchProgress + ""
        );
    }

    public Long get(Long videoId) {
        var time = redisAsyncSetter.get(
            stringRedisTemplate,
            RedisConstant.PLAIN_USER_LAST_WATCH_TIME + UserContext.getUserId() + ":" + videoId,
            () -> {
                var obj = plainUserLastWatchTimeMapper.selectOne(new LambdaQueryWrapper<PlainUserLastWatchTime>()
                        .eq(PlainUserLastWatchTime::getUserId, UserContext.getUserId())
                        .eq(PlainUserLastWatchTime::getVideoId, videoId)
                        .select(PlainUserLastWatchTime::getLastWatchTime)
                );
                return Optional.ofNullable(obj)
                        .map(PlainUserLastWatchTime::getLastWatchTime)
                        .orElse(0L) + "";
            }
        );
        return Long.parseLong(time);
    }

    public void saveAll() {
        redisAsyncSetter.handleAll(
            stringRedisTemplate,
            RedisConstant.PLAIN_USER_LAST_WATCH_TIME + "*:*",
            RedisConstant.PLAIN_USER_LAST_WATCH_TIME.length(),
            (keys, values) -> {
                for (var i = 0; i < keys.size(); i++) {
                    var lastWatchTime = values.get(i);
                    if (lastWatchTime == null) continue;

                    // parse userId and videoId
                    var keyy = keys.get(i).split(":");
                    var userId = Long.parseLong(keyy[0]);
                    var videoId = Long.parseLong(keyy[1]);

                    if (plainUserLastWatchTimeMapper.update(new LambdaUpdateWrapper<PlainUserLastWatchTime>()
                            .eq(PlainUserLastWatchTime::getUserId, userId)
                            .eq(PlainUserLastWatchTime::getVideoId, videoId)
                            .set(PlainUserLastWatchTime::getLastWatchTime, Long.parseLong(lastWatchTime))
                    ) == 0) {
                        plainUserLastWatchTimeMapper.insert(new PlainUserLastWatchTime()
                                .setUserId(userId)
                                .setVideoId(videoId)
                                .setLastWatchTime(Long.parseLong(lastWatchTime))
                        );
                    }
                }
            },
            true
        );
    }
}
