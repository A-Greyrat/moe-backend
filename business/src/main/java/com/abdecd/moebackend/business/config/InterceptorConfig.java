package com.abdecd.moebackend.business.config;

import com.abdecd.moebackend.business.interceptor.RateLimitInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(@NotNull InterceptorRegistry registry) {
//        registry.addInterceptor(rateLimitInterceptor)
//                .addPathPatterns("/**");
    }
}
