package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.Result;
import com.dianping.entity.Voucher;

import java.util.List;

public interface VoucherService extends IService<Voucher> {

    /**
     * 添加秒杀券(落库后同步库存与活动时间到Redis)
     */
    Result addSeckillVoucher(Voucher voucher);

    /**
     * 查询商户优惠券列表
     */
    List<Voucher> listByShopId(Long shopId);

    /**
     * 查询全部有效秒杀券
     */
    List<Voucher> listSeckill();

    /**
     * 查询Redis中的剩余库存
     */
    Integer queryStock(Long voucherId);

    /**
     * 将秒杀券库存/元信息同步到Redis(幂等,应用启动时也会调用)
     */
    void syncStockToRedis(Voucher voucher);
}
