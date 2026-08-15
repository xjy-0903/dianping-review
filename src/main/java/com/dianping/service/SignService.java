package com.dianping.service;

import com.dianping.dto.Result;

public interface SignService {

    /**
     * 用户签到(Redis BitMap)
     */
    Result sign();

    /**
     * 连续签到天数(BITFIELD 位运算)
     */
    Result signCount();

    /**
     * 今日是否已签到
     */
    Result signStatus();

    /**
     * 本月签到日历
     */
    Result monthStatus();
}
