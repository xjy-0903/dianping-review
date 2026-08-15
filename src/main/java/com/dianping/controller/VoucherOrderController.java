package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.service.VoucherOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voucher-order")
public class VoucherOrderController {

    private final VoucherOrderService voucherOrderService;

    public VoucherOrderController(VoucherOrderService voucherOrderService) {
        this.voucherOrderService = voucherOrderService;
    }

    /**
     * 秒杀下单(需要请求头 X-User-Id)
     */
    @PostMapping("/seckill/{voucherId}")
    public Result seckill(@PathVariable Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    @GetMapping("/user")
    public Result userOrders() {
        return voucherOrderService.listUserOrders();
    }
}
