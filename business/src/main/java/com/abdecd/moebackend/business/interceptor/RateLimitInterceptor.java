package com.abdecd.moebackend.business.interceptor;

import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.lib.RateLimiter;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    @Autowired
    private RateLimiter rateLimiter;
    @Override
    public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {
        var realIp = request.getHeader("X-Real-IP");
        if (rateLimiter.isRateLimited(
                RedisConstant.LIMIT_IP_RATE + realIp,
                RedisConstant.LIMIT_IP_RATE_CNT,
                RedisConstant.LIMIT_IP_RATE_RESET_TIME,
                TimeUnit.SECONDS
        )) throw new BaseException(MessageConstant.RATE_LIMIT);
        return true;
    }
}
