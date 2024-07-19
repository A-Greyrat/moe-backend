package com.abdecd.moebackend.business.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private Integer port;
    @Value("${spring.data.redis.database:0}")
    private Integer database;
    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + host + ":" + port)
            .setDatabase(database)
            .setSubscriptionConnectionMinimumIdleSize(1)
            .setSubscriptionConnectionPoolSize(16)
            .setConnectionMinimumIdleSize(1)
            .setConnectionPoolSize(16);
        if (!password.isEmpty()) {
            config.useSingleServer().setPassword(password);
        }

        return Redisson.create(config);
    }
}
