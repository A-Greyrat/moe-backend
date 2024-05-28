package com.abdecd.moebackend.business.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    /**
     * 仅针对IO密集的任务
     */
    @Override
    public Executor getAsyncExecutor() {
//        return TtlExecutors.getTtlExecutor(new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10000)));
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public Executor slowExecutor() {
        return new ThreadPoolExecutor(10, 10, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10000));
    }
}
