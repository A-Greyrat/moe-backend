package com.abdecd.moebackend.business.tokenLogin.service;

import com.abdecd.moebackend.business.tokenLogin.common.TokenLoginConstant;
import com.abdecd.moebackend.business.tokenLogin.common.TokenLoginProp;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class LoginBlackListService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private TokenLoginProp tokenLoginProp;

    public void forceLogout(Long userId) {
        stringRedisTemplate.opsForValue().set(
            TokenLoginConstant.LOGIN_TOKEN_BLACKLIST + userId,
            new Date().getTime() + tokenLoginProp.getJwtTtlSeconds() * 1000L + "",
            tokenLoginProp.getJwtTtlSeconds(),
            TimeUnit.SECONDS
        );
    }

    public boolean checkInBlackList(long userId, long tokenTtlms) {
        var timestamp = stringRedisTemplate.opsForValue().get(TokenLoginConstant.LOGIN_TOKEN_BLACKLIST + userId);
        if (timestamp == null) return false;
        return Long.parseLong(timestamp) > tokenTtlms;
    }
}
