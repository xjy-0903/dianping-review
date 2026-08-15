package com.dianping.service.impl;

import com.dianping.dto.Result;
import com.dianping.service.SignService;
import com.dianping.utils.RedisConstants;
import com.dianping.utils.UserHolder;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SignServiceImpl implements SignService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final StringRedisTemplate stringRedisTemplate;

    public SignServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result sign() {
        Long userId = UserHolder.get();
        if (userId == null) {
            return Result.fail("请先登录,并在请求头携带 X-User-Id");
        }
        LocalDate now = LocalDate.now();
        String key = signKey(userId, now);
        stringRedisTemplate.opsForValue().setBit(key, now.getDayOfMonth() - 1L, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        Long userId = UserHolder.get();
        if (userId == null) {
            return Result.fail("请先登录,并在请求头携带 X-User-Id");
        }
        LocalDate now = LocalDate.now();
        int day = now.getDayOfMonth();
        String key = signKey(userId, now);
        List<Long> result = stringRedisTemplate.opsForValue().bitField(key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(day))
                        .valueAt(0));
        if (result == null || result.isEmpty() || result.get(0) == null) {
            return Result.ok(0);
        }
        long mask = result.get(0);
        int count = 0;
        while ((mask & 1) == 1) {
            count++;
            mask >>>= 1;
        }
        return Result.ok(count);
    }

    @Override
    public Result signStatus() {
        Long userId = UserHolder.get();
        if (userId == null) {
            return Result.fail("请先登录,并在请求头携带 X-User-Id");
        }
        LocalDate now = LocalDate.now();
        Boolean signed = stringRedisTemplate.opsForValue()
                .getBit(signKey(userId, now), now.getDayOfMonth() - 1L);
        return Result.ok(Boolean.TRUE.equals(signed));
    }

    @Override
    public Result monthStatus() {
        Long userId = UserHolder.get();
        if (userId == null) {
            return Result.fail("请先登录,并在请求头携带 X-User-Id");
        }
        LocalDate now = LocalDate.now();
        String key = signKey(userId, now);
        Map<Integer, Boolean> calendar = new LinkedHashMap<>();
        int days = YearMonth.from(now).lengthOfMonth();
        for (int day = 1; day <= days; day++) {
            calendar.put(day, Boolean.TRUE.equals(
                    stringRedisTemplate.opsForValue().getBit(key, day - 1L)));
        }
        return Result.ok(calendar);
    }

    private String signKey(Long userId, LocalDate date) {
        return RedisConstants.SIGN_KEY + userId + ":" + date.format(MONTH_FORMATTER);
    }
}
