package com.dianping.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * 多级缓存客户端(Cache Aside Pattern)
 * - queryWithPassThrough: 缓存穿透防护(空值缓存) + 随机TTL(防雪崩)
 * - queryWithMutex: 互斥锁(SETNX + 令牌校验解锁)解决热点key缓存击穿
 * - delete/set: 主动更新缓存
 */
@Slf4j
@Component
public class CacheClient {

    private static final int MUTEX_MAX_RETRY = 200;
    private static final long MUTEX_SLEEP_MS = 50L;
    private static final Duration MUTEX_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<Long> unlockScript;
    private final String lockToken = UUID.randomUUID().toString();

    public CacheClient(StringRedisTemplate stringRedisTemplate,
                       ObjectMapper objectMapper,
                       DefaultRedisScript<Long> unlockScript) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.unlockScript = unlockScript;
    }

    public <T> T queryWithPassThrough(String keyPrefix, Long id, Class<T> type,
                                      Function<Long, T> dbFallback, Duration ttl, Duration nullTtl) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json != null) {
            return json.isEmpty() ? null : deserialize(json, type, key);
        }
        T value = dbFallback.apply(id);
        if (value == null) {
            stringRedisTemplate.opsForValue().set(key, "", nullTtl);
            return null;
        }
        setWithRandomTtl(key, value, ttl);
        return value;
    }

    public <T> T queryWithMutex(String keyPrefix, Long id, Class<T> type,
                                Function<Long, T> dbFallback, Duration ttl) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json != null) {
            return json.isEmpty() ? null : deserialize(json, type, key);
        }
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        try {
            for (int i = 0; i < MUTEX_MAX_RETRY; i++) {
                if (tryLock(lockKey)) {
                    try {
                        json = stringRedisTemplate.opsForValue().get(key);
                        if (json != null) {
                            return json.isEmpty() ? null : deserialize(json, type, key);
                        }
                        T value = dbFallback.apply(id);
                        if (value == null) {
                            stringRedisTemplate.opsForValue().set(key, "", Duration.ofMinutes(2));
                            return null;
                        }
                        setWithRandomTtl(key, value, ttl);
                        return value;
                    } finally {
                        unlock(lockKey);
                    }
                }
                Thread.sleep(MUTEX_SLEEP_MS);
            }
            throw new RuntimeException("获取互斥锁超时: " + lockKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待互斥锁被中断: " + lockKey, e);
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化缓存对象失败: " + key, e);
        }
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    private void setWithRandomTtl(String key, Object value, Duration ttl) {
        set(key, value, ttl.plusSeconds(ThreadLocalRandom.current().nextInt(60)));
    }

    private <T> T deserialize(String json, Class<T> type, String key) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.warn("缓存反序列化失败,删除缓存 key={}", key, e);
            stringRedisTemplate.delete(key);
            return null;
        }
    }

    private boolean tryLock(String key) {
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, lockToken, MUTEX_TTL);
        return Boolean.TRUE.equals(ok);
    }

    private void unlock(String key) {
        stringRedisTemplate.execute(unlockScript, List.of(key), lockToken);
    }
}
