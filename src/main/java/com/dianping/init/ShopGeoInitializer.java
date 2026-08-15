package com.dianping.init;

import com.dianping.entity.Shop;
import com.dianping.mapper.ShopMapper;
import com.dianping.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动时把商户经纬度写入 Redis GEO(保证已有商户可被附近搜索命中)
 */
@Slf4j
@Component
public class ShopGeoInitializer implements ApplicationRunner {

    private final ShopMapper shopMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public ShopGeoInitializer(ShopMapper shopMapper, StringRedisTemplate stringRedisTemplate) {
        this.shopMapper = shopMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        stringRedisTemplate.delete(RedisConstants.GEO_SHOP_KEY);
        List<Shop> shops = shopMapper.selectList(null);
        for (Shop shop : shops) {
            stringRedisTemplate.opsForGeo().add(RedisConstants.GEO_SHOP_KEY,
                    new Point(shop.getX(), shop.getY()), String.valueOf(shop.getId()));
        }
        log.info("商户GEO坐标写入完成,共{}家商户", shops.size());
    }
}
