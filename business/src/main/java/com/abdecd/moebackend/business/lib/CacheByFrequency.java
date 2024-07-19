package com.abdecd.moebackend.business.lib;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class CacheByFrequency<T> {
    private final RedisTemplate<String, T> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final String rootKey;
    private final int maxCount;
    private final Integer ttlSeconds;

    public CacheByFrequency(
            RedisTemplate<String, T> redisTemplate,
            StringRedisTemplate stringRedisTemplate,
            RedissonClient redissonClient,
            @Nonnull String rootKey,
            int maxCount,
            Integer frequencyTtlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
        this.rootKey = rootKey;
        this.maxCount = maxCount;
        this.ttlSeconds = frequencyTtlSeconds;
    }

    protected List<String> scan(String pattern) {
        var keys = new ArrayList<String>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(10)
                .build();
        try (var iter = stringRedisTemplate.scan(options)) {
            while (iter.hasNext()) {
                keys.add(iter.next());
            }
        }
        return keys;
    }

    /**
     * 记录访问次数 调用后对应次数+1
     * @param frequencyKey 访问对应的key 同一个key访问次数累加
     */
    public void recordFrequency(String frequencyKey) {
        var scriptText = """
                -- 定义变量
                local rootKey = ARGV[1]
                local key = ARGV[2]
                local ttlSeconds = tonumber(ARGV[3])

                -- 检查根键对应的有序集合是否存在
                if not redis.call('exists', rootKey .. ":zSet") then
                    -- 如果不存在，则添加元素到有序集合中，score 设为 1
                    redis.call('zadd', rootKey .. ":zSet", 1, key)
                else
                    -- 将指定 key 在有序集合中的 score 增加 1
                    redis.call('zincrby', rootKey .. ":zSet", 1, key)
                end
                
                local t = redis.call('ttl',rootKey .. ":zSet");
                if t == -1 then
                    redis.call('expire', rootKey .. ":zSet", ttlSeconds)
                end;
                """;
        var script = new DefaultRedisScript<>(scriptText, Long.class);
        stringRedisTemplate.execute(script, Collections.emptyList(), rootKey, frequencyKey, ttlSeconds + "");
    }

    /**
     * 获取内容
     * @param key 键值一一对应
     * @param failCb 未命中缓存回调
     * @param keyToFrequencyKeyFunc 将key转换为frequencyKey 用于判断是否应缓存
     * @param keyTtlSeconds 键值对应的缓存时间
     * @return 值
     */
    @Nullable public T get(
            String key,
            @Nonnull Supplier<T> failCb,
            @Nullable Function<String, String> keyToFrequencyKeyFunc,
            @Nullable Integer keyTtlSeconds
    ) {
        if (keyToFrequencyKeyFunc == null) keyToFrequencyKeyFunc = k -> k;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rootKey + ":value:" + key))) {
            return redisTemplate.opsForValue().get(rootKey + ":value:" + key);
        } else {
            // 判断是否可缓
            var set = stringRedisTemplate.opsForZSet().reverseRange(rootKey + ":zSet", 0, maxCount);
            if (set == null || !set.contains(keyToFrequencyKeyFunc.apply(key))) {
                return failCb.get();
            } else {
                // 去数据库拿数据并缓存
                RLock lock = redissonClient.getLock(rootKey + ":lock");
                lock.lock();
                try {
                    // 第一个拿过了后面直接走缓存
                    if (Boolean.TRUE.equals(redisTemplate.hasKey(rootKey + ":value:" + key)))
                        return redisTemplate.opsForValue().get(rootKey + ":value:" + key);
                    redisTemplate.opsForValue().set(rootKey + ":value:" + key, failCb.get());
                    redisTemplate.expire(
                            rootKey + ":value:" + key,
                            Objects.requireNonNullElseGet(keyTtlSeconds, () -> ttlSeconds * 2),
                            TimeUnit.SECONDS
                    );
                    clearUnusedOne(keyToFrequencyKeyFunc);
                } finally {
                    lock.unlock();
                }
                return redisTemplate.opsForValue().get(rootKey + ":value:" + key);
            }
        }
    }

    public void delete(String key) {
        RLock lock = redissonClient.getLock(rootKey + ":lock");
        lock.lock();
        try {
            redisTemplate.delete(rootKey + ":value:" + key);
        } finally {
            lock.unlock();
        }
    }

    public void deleteMany(String pattern) {
        RLock lock = redissonClient.getLock(rootKey + ":lock");
        lock.lock();
        try {
            var keys = scan(rootKey + ":value:" + pattern);
            redisTemplate.delete(keys);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清理缓存 随机清掉一个
     * @param keyToFrequencyKeyFunc key的判断函数，用于判断是否可缓
     */
    private void clearUnusedOne(@Nullable Function<String, String> keyToFrequencyKeyFunc) {
        if (keyToFrequencyKeyFunc == null) keyToFrequencyKeyFunc = k -> k;
        var set = stringRedisTemplate.opsForZSet().reverseRange(rootKey + ":zSet", 0, maxCount);
        if (set == null) return;

        var fullKeys = scan(rootKey + ":value:*");
        if (fullKeys.isEmpty()) return;

        var needDeleted = new ArrayList<String>();
        Function<String, String> finalValueKeyToZSetKeyFunc = keyToFrequencyKeyFunc;
        fullKeys.forEach(fullKey -> {
            if (!set.contains(
                    finalValueKeyToZSetKeyFunc.apply(fullKey.substring(fullKey.indexOf(":value:")+":value:".length()))
            )) needDeleted.add(fullKey);
        });
        if (needDeleted.isEmpty()) return;
        Collections.shuffle(needDeleted);
        RLock lock = redissonClient.getLock(rootKey + ":lock");
        lock.lock();
        try {
            redisTemplate.delete(needDeleted.getFirst());
        } finally {
            lock.unlock();
        }
    }
}
