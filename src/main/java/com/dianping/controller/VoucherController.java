package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.entity.Voucher;
import com.dianping.service.VoucherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voucher")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    /**
     * 添加秒杀券(同步库存到Redis)
     */
    @PostMapping("/seckill")
    public Result addSeckill(@RequestBody Voucher voucher) {
        return voucherService.addSeckillVoucher(voucher);
    }

    @GetMapping("/list/{shopId}")
    public Result list(@PathVariable Long shopId) {
        return Result.ok(voucherService.listByShopId(shopId));
    }

    /**
     * 全部有效秒杀券(秒杀中心页)
     */
    @GetMapping("/seckill/list")
    public Result seckillList() {
        return Result.ok(voucherService.listSeckill());
    }

    @GetMapping("/seckill/stock/{voucherId}")
    public Result stock(@PathVariable Long voucherId) {
        Integer stock = voucherService.queryStock(voucherId);
        return stock == null ? Result.fail("秒杀券不存在或已过期") : Result.ok(stock);
    }
}
