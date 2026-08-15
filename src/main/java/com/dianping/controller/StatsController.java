package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.service.StatsService;
import com.dianping.utils.UserHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @PostMapping("/uv/record/{shopId}")
    public Result record(@PathVariable Long shopId) {
        Long userId = UserHolder.get();
        statsService.recordUv(shopId, userId == null ? "anonymous" : String.valueOf(userId));
        return Result.ok();
    }

    @GetMapping("/uv/{shopId}")
    public Result uvCount(@PathVariable Long shopId) {
        return Result.ok(statsService.uvCount(shopId));
    }
}
