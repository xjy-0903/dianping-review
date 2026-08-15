package com.dianping.mq;

import com.dianping.service.VoucherOrderService;
import com.dianping.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.stream.ByteRecord;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 秒杀订单异步消费者(基于 Redis Stream + 消费者组)
 * - 主循环: XREADGROUP 阻塞拉取新消息
 * - 失败重试: 消息重新入队(携带retry计数),超过最大重试次数回滚Redis库存
 * - PEL恢复: 定时读取 Pending 列表,兜底处理消费者宕机导致的未确认消息
 * - 幂等保证: 订单表(user_id, voucher_id)唯一索引 + 主键去重
 */
@Slf4j
@Component
public class SeckillOrderConsumer implements ApplicationRunner {

    private static final String CONSUMER_NAME = "consumer-" + UUID.randomUUID().toString().substring(0, 8);

    private final StringRedisTemplate stringRedisTemplate;
    private final VoucherOrderService voucherOrderService;

    @Value("${dianping.seckill.max-retry:5}")
    private int maxRetry;

    private volatile boolean running = true;

    public SeckillOrderConsumer(StringRedisTemplate stringRedisTemplate, VoucherOrderService voucherOrderService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.voucherOrderService = voucherOrderService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureGroup();
        Thread thread = new Thread(this::consumeLoop, "seckill-order-consumer");
        thread.setDaemon(true);
        thread.start();
        log.info("秒杀订单消费者启动 group={}, consumer={}", RedisConstants.STREAM_ORDER_GROUP, CONSUMER_NAME);
    }

    @PreDestroy
    public void shutdown() {
        running = false;
    }

    private void ensureGroup() {
        try {
            stringRedisTemplate.execute((RedisCallback<String>) connection ->
                    connection.streamCommands().xGroupCreate(
                            serialize(RedisConstants.STREAM_ORDER_KEY),
                            RedisConstants.STREAM_ORDER_GROUP,
                            ReadOffset.from("0"),
                            true));
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.info("消费者组已存在,无需重复创建");
            } else {
                throw new IllegalStateException("创建消费者组失败", e);
            }
        }
    }

    private void consumeLoop() {
        while (running) {
            try {
                List<ByteRecord> records = stringRedisTemplate.execute((RedisCallback<List<ByteRecord>>) connection ->
                        connection.streamCommands().xReadGroup(
                                Consumer.from(RedisConstants.STREAM_ORDER_GROUP, CONSUMER_NAME),
                                StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                                StreamOffset.create(serialize(RedisConstants.STREAM_ORDER_KEY), ReadOffset.lastConsumed())));
                if (records != null) {
                    for (ByteRecord record : records) {
                        handle(record.getId(), record.getValue());
                    }
                }
            } catch (Exception e) {
                log.error("消费秒杀订单消息异常", e);
                sleepQuietly(1000);
            }
        }
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void recoverPending() {
        if (!running) {
            return;
        }
        try {
            List<ByteRecord> pending = stringRedisTemplate.execute((RedisCallback<List<ByteRecord>>) connection ->
                    connection.streamCommands().xReadGroup(
                            Consumer.from(RedisConstants.STREAM_ORDER_GROUP, CONSUMER_NAME),
                            StreamReadOptions.empty().count(50),
                            StreamOffset.create(serialize(RedisConstants.STREAM_ORDER_KEY), ReadOffset.from("0"))));
            if (pending != null && !pending.isEmpty()) {
                log.info("恢复处理PEL中的{}条未确认消息", pending.size());
                for (ByteRecord record : pending) {
                    handle(record.getId(), record.getValue());
                }
            }
        } catch (Exception e) {
            log.error("恢复PEL消息异常", e);
        }
    }

    private void handle(RecordId recordId, Map<byte[], byte[]> body) {
        Map<String, String> fields = new HashMap<>();
        body.forEach((k, v) -> fields.put(new String(k, StandardCharsets.UTF_8),
                new String(v, StandardCharsets.UTF_8)));
        try {
            Long orderId = Long.parseLong(fields.get("orderId"));
            Long userId = Long.parseLong(fields.get("userId"));
            Long voucherId = Long.parseLong(fields.get("voucherId"));
            voucherOrderService.createOrderAfterSeckill(orderId, userId, voucherId);
            ack(recordId);
        } catch (Exception e) {
            int retry = Integer.parseInt(fields.getOrDefault("retry", "0"));
            if (retry < maxRetry) {
                fields.put("retry", String.valueOf(retry + 1));
                add(fields);
                log.warn("订单处理失败,重新入队第{}次重试 orderId={}", retry + 1, fields.get("orderId"));
            } else {
                rollback(fields);
                log.error("订单处理失败且重试次数超限,已回滚Redis库存 fields={}", fields, e);
            }
            ack(recordId);
        }
    }

    private void add(Map<String, String> fields) {
        stringRedisTemplate.execute((RedisCallback<RecordId>) connection -> {
            Map<byte[], byte[]> body = new HashMap<>();
            fields.forEach((k, v) -> body.put(serialize(k), serialize(v)));
            return connection.streamCommands()
                    .xAdd(serialize(RedisConstants.STREAM_ORDER_KEY), body);
        });
    }

    private void ack(RecordId recordId) {
        stringRedisTemplate.execute((RedisCallback<Long>) connection ->
                connection.streamCommands().xAck(serialize(RedisConstants.STREAM_ORDER_KEY),
                        RedisConstants.STREAM_ORDER_GROUP, recordId));
    }

    private void rollback(Map<String, String> fields) {
        stringRedisTemplate.opsForValue()
                .increment(RedisConstants.SECKILL_STOCK_KEY + fields.get("voucherId"));
        stringRedisTemplate.opsForSet()
                .remove(RedisConstants.SECKILL_ORDER_SET_KEY + fields.get("voucherId"), fields.get("userId"));
    }

    private byte[] serialize(String value) {
        return stringRedisTemplate.getStringSerializer().serialize(value);
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
