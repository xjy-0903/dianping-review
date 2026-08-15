package com.dianping.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.entity.Voucher;
import com.dianping.mapper.VoucherMapper;
import com.dianping.service.VoucherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用启动时把数据库中的有效秒杀券库存同步到Redis(服务重启后秒杀数据自动恢复)
 */
@Slf4j
@Component
public class SeckillStockInitializer implements ApplicationRunner {

    private final VoucherMapper voucherMapper;
    private final VoucherService voucherService;

    public SeckillStockInitializer(VoucherMapper voucherMapper, VoucherService voucherService) {
        this.voucherMapper = voucherMapper;
        this.voucherService = voucherService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Voucher> vouchers = voucherMapper.selectList(new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getType, 1)
                .gt(Voucher::getEndTime, LocalDateTime.now()));
        for (Voucher voucher : vouchers) {
            voucherService.syncStockToRedis(voucher);
        }
        log.info("秒杀券库存同步完成,共{}张有效秒杀券", vouchers.size());
    }
}
