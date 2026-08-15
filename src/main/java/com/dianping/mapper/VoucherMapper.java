package com.dianping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianping.entity.Voucher;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface VoucherMapper extends BaseMapper<Voucher> {

    /**
     * 条件扣减库存(WHERE stock > 0 从数据库层杜绝超卖)
     *
     * @return 受影响行数,0 表示库存不足
     */
    @Update("UPDATE tb_voucher SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
    int deductStock(@Param("id") Long id);
}
