package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.Result;
import com.dianping.entity.VoucherOrder;

public interface VoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀下单: Lua脚本预扣库存 -> 生成订单号 -> 投递Redis Stream异步队列
     */
    Result seckillVoucher(Long voucherId);

    /**
     * 消费者回调: 异步落库(幂等)
     */
    void createOrderAfterSeckill(Long orderId, Long userId, Long voucherId);

    /**
     * 当前用户订单列表
     */
    Result listUserOrders();
}
