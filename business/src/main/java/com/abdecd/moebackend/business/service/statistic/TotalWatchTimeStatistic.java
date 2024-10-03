package com.abdecd.moebackend.business.service.statistic;

import com.abdecd.moebackend.business.dao.entity.PlainUserTotalWatchTime;
import com.abdecd.moebackend.business.dao.mapper.PlainUserTotalWatchTimeMapper;
import com.abdecd.moebackend.business.lib.RedisAsyncSetter;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TotalWatchTimeStatistic {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PlainUserTotalWatchTimeMapper plainUserTotalWatchTimeMapper;
    @Autowired
    private RedisAsyncSetter<String> redisAsyncSetter;

    public void add(Long videoId, int addTime) {
        var oldT = get(videoId);
        redisAsyncSetter.set(
                stringRedisTemplate,
                RedisConstant.PLAIN_USER_TOTAL_WATCH_TIME + UserContext.getUserId() + ":" + videoId,
                oldT + addTime + ""
        );
    }

    @Nonnull
    public Long get(Long videoId) {
        var time = redisAsyncSetter.get(
            stringRedisTemplate,
            RedisConstant.PLAIN_USER_TOTAL_WATCH_TIME + UserContext.getUserId() + ":" + videoId,
            () -> {
                var obj = plainUserTotalWatchTimeMapper.selectOne(new LambdaQueryWrapper<PlainUserTotalWatchTime>()
                        .eq(PlainUserTotalWatchTime::getUserId, UserContext.getUserId())
                        .eq(PlainUserTotalWatchTime::getVideoId, videoId)
                        .select(PlainUserTotalWatchTime::getTotalWatchTime)
                );
                return Optional.ofNullable(obj)
                        .map(PlainUserTotalWatchTime::getTotalWatchTime)
                        .orElse(0L) + "";
            }
        );
        return Long.parseLong(time);
    }

    public void saveAll() {
        redisAsyncSetter.handleAll(
            stringRedisTemplate,
            RedisConstant.PLAIN_USER_TOTAL_WATCH_TIME + "*:*",
            RedisConstant.PLAIN_USER_TOTAL_WATCH_TIME.length(),
            (keys, values) -> {
                for (int i = 0; i < keys.size(); i++) {
                    var totalWatchTime = values.get(i);
                    if (totalWatchTime == null) continue;

                    // parse userId and videoId
                    var keyy = keys.get(i).split(":");
                    var userId = Long.parseLong(keyy[0]);
                    var videoId = Long.parseLong(keyy[1]);

                    if (plainUserTotalWatchTimeMapper.update(new LambdaUpdateWrapper<PlainUserTotalWatchTime>()
                            .eq(PlainUserTotalWatchTime::getUserId, userId)
                            .eq(PlainUserTotalWatchTime::getVideoId, videoId)
                            .set(PlainUserTotalWatchTime::getTotalWatchTime, Long.parseLong(totalWatchTime))
                    ) == 0) {
                        plainUserTotalWatchTimeMapper.insert(new PlainUserTotalWatchTime()
                                .setUserId(userId)
                                .setVideoId(videoId)
                                .setTotalWatchTime(Long.parseLong(totalWatchTime))
                        );
                    }
                }
            },
            true
        );
    }
}
