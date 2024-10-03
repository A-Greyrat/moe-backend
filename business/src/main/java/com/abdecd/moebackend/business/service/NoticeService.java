package com.abdecd.moebackend.business.service;

import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.pojo.vo.notice.Notice;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NoticeService {
    final int MAX_NOTICE_CNT = 20;
    private RedisTemplate<String, List<Notice>> redisTemplate;

    @Autowired
    public void setRedisTemplate(RedisConnectionFactory connectionFactory) {
        this.redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();
    }

    public void addNotice(int index, Notice notice) {
        var list = redisTemplate.opsForValue().get(RedisConstant.ANNOUNCEMENT);
        if (list == null) list = new ArrayList<>();
        if (list.size() >= MAX_NOTICE_CNT) throw new BaseException(MessageConstant.MAX_SIZE);
        try {
            list.add(index, notice);
        } catch (IndexOutOfBoundsException e) {
            throw new BaseException(MessageConstant.ARRAY_INDEX_OUT_OF_BOUNDS + ": " + e.getMessage());
        }
        redisTemplate.opsForValue().set(RedisConstant.ANNOUNCEMENT, new ArrayList<>(list));
    }

    public void addNoticeAtFirst(Notice notice) {
        var list = redisTemplate.opsForValue().get(RedisConstant.ANNOUNCEMENT);
        if (list == null) {
            list = new ArrayList<>();
            list.add(notice);
        } else {
            if (list.size() >= MAX_NOTICE_CNT) throw new BaseException(MessageConstant.MAX_SIZE);
            list.addFirst(notice);
        }
        redisTemplate.opsForValue().set(RedisConstant.ANNOUNCEMENT, new ArrayList<>(list));
    }

    public void deleteNotice(int index) {
        var list = redisTemplate.opsForValue().get(RedisConstant.ANNOUNCEMENT);
        if (list == null) throw new BaseException("notice not found");
        try {
            list.remove(index);
        } catch (IndexOutOfBoundsException e) {
            throw new BaseException(MessageConstant.ARRAY_INDEX_OUT_OF_BOUNDS + ": " + e.getMessage());
        }
        redisTemplate.opsForValue().set(RedisConstant.ANNOUNCEMENT, new ArrayList<>(list));
    }

    public void updateNotice(int index, Notice notice) {
        var list = redisTemplate.opsForValue().get(RedisConstant.ANNOUNCEMENT);
        if (list == null) throw new BaseException("notice not found");
        try {
            list.set(index, notice);
        } catch (IndexOutOfBoundsException e) {
            throw new BaseException(MessageConstant.ARRAY_INDEX_OUT_OF_BOUNDS + ": " + e.getMessage());
        }
        redisTemplate.opsForValue().set(RedisConstant.ANNOUNCEMENT, new ArrayList<>(list));
    }

    public List<Notice> getNoticeList() {
        var list = redisTemplate.opsForValue().get(RedisConstant.ANNOUNCEMENT);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }
}
