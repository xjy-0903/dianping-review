package com.dianping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.entity.Voucher;
import com.dianping.mapper.VoucherMapper;
import com.dianping.service.VoucherService;
import com.dianping.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements VoucherService {

    private final StringRedisTemplate stringRedisTemplate;

    public VoucherServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result addSeckillVoucher(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getBeginTime() == null || voucher.getEndTime() == null) {
            return Result.fail("秒杀券必须设置开始/结束时间");
        }
        if (!voucher.getEndTime().isAfter(now)) {
            return Result.fail("结束时间必须晚于当前时间");
        }
        if (voucher.getStock() == null || voucher.getStock() <= 0) {
            return Result.fail("库存必须大于0");
        }
        voucher.setType(1);
        voucher.setCreateTime(now);
        save(voucher);
        syncStockToRedis(voucher);
        return Result.ok(voucher.getId());
    }

    @Override
    public List<Voucher> listByShopId(Long shopId) {
        return list(new LambdaQueryWrapper<Voucher>().eq(Voucher::getShopId, shopId));
    }

    @Override
    public List<Voucher> listSeckill() {
        return list(new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getType, 1)
                .gt(Voucher::getEndTime, LocalDateTime.now())
                .orderByAsc(Voucher::getEndTime));
    }

    @Override
    public Integer queryStock(Long voucherId) {
        String stock = stringRedisTemplate.opsForValue().get(RedisConstants.SECKILL_STOCK_KEY + voucherId);
        return stock == null ? null : Integer.parseInt(stock);
    }

    @Override
    public void syncStockToRedis(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        String stockKey = RedisConstants.SECKILL_STOCK_KEY + voucher.getId();
        stringRedisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(voucher.getStock()),
                Duration.between(now, voucher.getEndTime()).plusMinutes(1));
        String metaKey = RedisConstants.SECKILL_META_KEY + voucher.getId();
        Map<String, String> meta = new HashMap<>();
        meta.put("beginTime", String.valueOf(toEpochMilli(voucher.getBeginTime())));
        meta.put("endTime", String.valueOf(toEpochMilli(voucher.getEndTime())));
        stringRedisTemplate.opsForHash().putAll(metaKey, meta);
        stringRedisTemplate.expire(metaKey, Duration.between(now, voucher.getEndTime()).plusDays(1));
    }

    private long toEpochMilli(LocalDateTime time) {
        return time.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }
}
