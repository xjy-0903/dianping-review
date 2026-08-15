package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.NearbyShopDTO;
import com.dianping.entity.Shop;

import java.util.List;

public interface ShopService extends IService<Shop> {

    /**
     * 缓存穿透防护 + 旁路缓存查询
     */
    Shop queryById(Long id);

    /**
     * 互斥锁防缓存击穿查询
     */
    Shop queryByIdWithMutex(Long id);

    /**
     * 更新商户(先更新DB,再删除缓存)
     */
    boolean updateShop(Shop shop);

    /**
     * 新增商户(写入GEO)
     */
    Long saveShop(Shop shop);

    /**
     * 附近商户搜索(Redis GEO)
     */
    List<NearbyShopDTO> queryNearby(double x, double y, double distanceKm);
}
