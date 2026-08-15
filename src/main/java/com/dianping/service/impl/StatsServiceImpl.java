package com.dianping.service.impl;

import com.dianping.service.StatsService;
import com.dianping.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class StatsServiceImpl implements StatsService {

    private final StringRedisTemplate stringRedisTemplate;

    public StatsServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void recordUv(Long shopId, String userId) {
        stringRedisTemplate.opsForHyperLogLog().add(RedisConstants.UV_SHOP_KEY + shopId, userId);
    }

    @Override
    public long uvCount(Long shopId) {
        Long size = stringRedisTemplate.opsForHyperLogLog().size(RedisConstants.UV_SHOP_KEY + shopId);
        return size == null ? 0 : size;
    }
}
