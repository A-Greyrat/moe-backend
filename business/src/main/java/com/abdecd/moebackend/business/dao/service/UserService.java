package com.abdecd.moebackend.business.dao.service;

import com.abdecd.moebackend.business.dao.dataencrypt.EncryptStrHandler;
import com.abdecd.moebackend.business.dao.entity.User;
import com.abdecd.moebackend.business.dao.mapper.UserMapper;
import com.abdecd.moebackend.business.lib.CacheByFrequency;
import com.abdecd.moebackend.business.lib.CacheByFrequencyFactory;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    @Autowired
    private UserMapper userMapper;
    @Getter
    private CacheByFrequency<User> userCache;
    @Getter
    private CacheByFrequency<Long> userEmailKeyCache;

    @Autowired
    public void setUserCache(CacheByFrequencyFactory factory) {
        userCache = factory.create(RedisConstant.USER, 10000, 180 * 86400);
        userEmailKeyCache = factory.create(RedisConstant.USER_EMAIL_KEY, 10000, 180 * 86400);
    }

    public User getUserById(Long userId) {
        var user = userCache.get(
            userId + "",
            () -> userMapper.selectById(userId),
            null,
            null
        );
        if (user != null) userCache.recordFrequency(userId + "");
        return user;
    }

    public User getUserByEmail(String rawEmail) {
        var id = userEmailKeyCache.get(
            rawEmail,
            () -> {
                var user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, EncryptStrHandler.encrypt(rawEmail))
                );
                return user == null ? null : user.getId();
            },
            null,
            null
        );
        if (id != null) {
            userEmailKeyCache.recordFrequency(rawEmail);
            return getUserById(id);
        }
        return null;
    }
}
