package com.dianping.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.NearbyShopDTO;
import com.dianping.entity.Shop;
import com.dianping.mapper.ShopMapper;
import com.dianping.service.ShopService;
import com.dianping.service.StatsService;
import com.dianping.utils.CacheClient;
import com.dianping.utils.RedisConstants;
import com.dianping.utils.UserHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    private final CacheClient cacheClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final StatsService statsService;

    @Value("${dianping.cache.shop-ttl:30m}")
    private Duration shopTtl;

    @Value("${dianping.cache.null-ttl:2m}")
    private Duration nullTtl;

    public ShopServiceImpl(CacheClient cacheClient, StringRedisTemplate stringRedisTemplate, StatsService statsService) {
        this.cacheClient = cacheClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.statsService = statsService;
    }

    @Override
    public Shop queryById(Long id) {
        Shop shop = cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,
                baseMapper::selectById, shopTtl, nullTtl);
        if (shop != null) {
            Long userId = UserHolder.get();
            statsService.recordUv(id, userId == null ? "anonymous" : String.valueOf(userId));
        }
        return shop;
    }

    @Override
    public Shop queryByIdWithMutex(Long id) {
        return cacheClient.queryWithMutex(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,
                baseMapper::selectById, shopTtl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateShop(Shop shop) {
        boolean success = updateById(shop);
        if (success) {
            cacheClient.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveShop(Shop shop) {
        save(shop);
        if (shop.getX() != null && shop.getY() != null) {
            stringRedisTemplate.opsForGeo().add(RedisConstants.GEO_SHOP_KEY,
                    new Point(shop.getX(), shop.getY()), String.valueOf(shop.getId()));
        }
        return shop.getId();
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<NearbyShopDTO> queryNearby(double x, double y, double distanceKm) {
        GeoResults<GeoLocation<String>> results = stringRedisTemplate.opsForGeo().radius(
                RedisConstants.GEO_SHOP_KEY,
                new Circle(new Point(x, y), new Distance(distanceKm, Metrics.KILOMETERS)),
                GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().sortAscending());
        if (results == null || results.getContent().isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Double> distances = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        for (GeoResult<GeoLocation<String>> result : results.getContent()) {
            GeoLocation<String> location = result.getContent();
            Long shopId = Long.parseLong(location.getName());
            ids.add(shopId);
            distances.put(location.getName(), result.getDistance().getValue());
        }
        List<Shop> shops = listByIds(ids);
        Map<Long, Shop> shopMap = shops.stream().collect(Collectors.toMap(Shop::getId, s -> s));
        List<NearbyShopDTO> list = new ArrayList<>();
        for (Long id : ids) {
            Shop shop = shopMap.get(id);
            if (shop == null) {
                continue;
            }
            NearbyShopDTO dto = new NearbyShopDTO();
            dto.setId(shop.getId());
            dto.setName(shop.getName());
            dto.setAddress(shop.getAddress());
            dto.setScore(shop.getScore());
            dto.setAvgPrice(shop.getAvgPrice());
            dto.setSold(shop.getSold());
            dto.setDistanceKm(distances.get(String.valueOf(id)));
            list.add(dto);
            cacheClient.set(RedisConstants.CACHE_SHOP_KEY + id, shop, shopTtl);
        }
        return list;
    }
}
