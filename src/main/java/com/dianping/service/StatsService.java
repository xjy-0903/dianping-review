package com.dianping.service;

public interface StatsService {

    /**
     * 记录用户访问(HyperLogLog)
     */
    void recordUv(Long shopId, String userId);

    /**
     * 查询UV数
     */
    long uvCount(Long shopId);
}
