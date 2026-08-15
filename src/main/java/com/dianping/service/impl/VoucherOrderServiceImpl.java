package com.dianping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.entity.Voucher;
import com.dianping.entity.VoucherOrder;
import com.dianping.mapper.VoucherMapper;
import com.dianping.mapper.VoucherOrderMapper;
import com.dianping.service.VoucherOrderService;
import com.dianping.utils.RedisConstants;
import com.dianping.utils.RedisIdWorker;
import com.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final RedisIdWorker redisIdWorker;
    private final VoucherMapper voucherMapper;
    private final DefaultRedisScript<Long> seckillScript;

    public VoucherOrderServiceImpl(StringRedisTemplate stringRedisTemplate,
                                   RedissonClient redissonClient,
                                   RedisIdWorker redisIdWorker,
                                   VoucherMapper voucherMapper,
                                   DefaultRedisScript<Long> seckillScript) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
        this.redisIdWorker = redisIdWorker;
        this.voucherMapper = voucherMapper;
        this.seckillScript = seckillScript;
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.get();
        if (userId == null) {
            return Result.fail("请先登录,并在请求头携带 X-User-Id");
        }
        try {
            Long result = stringRedisTemplate.execute(seckillScript,
                    List.of(RedisConstants.SECKILL_STOCK_KEY + voucherId,
                            RedisConstants.SECKILL_ORDER_SET_KEY + voucherId,
                            RedisConstants.SECKILL_META_KEY + voucherId),
                    String.valueOf(userId), String.valueOf(System.currentTimeMillis()));
            if (result == null) {
                throw new IllegalStateException("Lua脚本返回为空");
            }
            if (result == 1) {
                return Result.fail("库存不足");
            }
            if (result == 2) {
                return Result.fail("您已抢购过该券,请勿重复下单");
            }
            if (result == 3) {
                return Result.fail("不在秒杀活动时间范围内");
            }
            long orderId = redisIdWorker.nextId("order");
            Map<String, String> fields = new HashMap<>();
            fields.put("orderId", String.valueOf(orderId));
            fields.put("userId", String.valueOf(userId));
            fields.put("voucherId", String.valueOf(voucherId));
            fields.put("retry", "0");
            publishToStream(fields);
            return Result.ok(orderId);
        } catch (Exception e) {
            log.warn("Redis秒杀链路异常,降级同步下单 voucherId={}, userId={}", voucherId, userId, e);
            return fallbackSyncOrder(voucherId, userId);
        }
    }

    /**
     * 降级同步路径: Redisson分布式锁保证集群模式下的一人一单
     * 不显式设置leaseTime,依赖 WatchDog 看门狗自动续期,防止业务阻塞导致锁提前释放
     */
    private Result fallbackSyncOrder(Long voucherId, Long userId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return Result.fail("优惠券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getBeginTime() != null && now.isBefore(voucher.getBeginTime())) {
            return Result.fail("秒杀尚未开始");
        }
        if (voucher.getEndTime() != null && now.isAfter(voucher.getEndTime())) {
            return Result.fail("秒杀已结束");
        }
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + userId);
        try {
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                return Result.fail("系统繁忙,请稍后重试");
            }
            Long count = baseMapper.selectCount(new LambdaQueryWrapper<VoucherOrder>()
                    .eq(VoucherOrder::getUserId, userId)
                    .eq(VoucherOrder::getVoucherId, voucherId));
            if (count != null && count > 0) {
                return Result.fail("您已抢购过该券,请勿重复下单");
            }
            int rows = voucherMapper.deductStock(voucherId);
            if (rows == 0) {
                return Result.fail("库存不足");
            }
            VoucherOrder order = new VoucherOrder();
            order.setId(redisIdWorker.nextId("order"));
            order.setUserId(userId);
            order.setVoucherId(voucherId);
            order.setStatus(0);
            order.setCreateTime(now);
            baseMapper.insert(order);
            return Result.ok(order.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.fail("系统繁忙,请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrderAfterSeckill(Long orderId, Long userId, Long voucherId) {
        VoucherOrder order = new VoucherOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        try {
            baseMapper.insert(order);
        } catch (DuplicateKeyException e) {
            log.info("订单已存在,幂等跳过 orderId={}", orderId);
            return;
        }
        int rows = voucherMapper.deductStock(voucherId);
        if (rows == 0) {
            throw new IllegalStateException("MySQL库存扣减失败 voucherId=" + voucherId);
        }
    }

    @Override
    public Result listUserOrders() {
        Long userId = UserHolder.get();
        if (userId == null) {
            return Result.fail("请先登录,并在请求头携带 X-User-Id");
        }
        return Result.ok(list(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getUserId, userId)
                .orderByDesc(VoucherOrder::getCreateTime)));
    }

    private void publishToStream(Map<String, String> fields) {
        stringRedisTemplate.execute((RedisCallback<RecordId>) connection -> {
            RedisSerializer<String> serializer = stringRedisTemplate.getStringSerializer();
            Map<byte[], byte[]> body = new HashMap<>();
            fields.forEach((k, v) -> body.put(serializer.serialize(k), serializer.serialize(v)));
            return connection.streamCommands()
                    .xAdd(serializer.serialize(RedisConstants.STREAM_ORDER_KEY), body);
        });
    }
}
