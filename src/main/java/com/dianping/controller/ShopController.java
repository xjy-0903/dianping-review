package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.entity.Shop;
import com.dianping.service.ShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * 商户详情(旁路缓存 + 穿透防护 + UV统计)
     */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable Long id) {
        Shop shop = shopService.queryById(id);
        return shop == null ? Result.fail("商户不存在") : Result.ok(shop);
    }

    /**
     * 商户详情(互斥锁防缓存击穿)
     */
    @GetMapping("/mutex/{id}")
    public Result queryByIdWithMutex(@PathVariable Long id) {
        Shop shop = shopService.queryByIdWithMutex(id);
        return shop == null ? Result.fail("商户不存在") : Result.ok(shop);
    }

    /**
     * 附近商户搜索(GEO),distance单位为km
     */
    @GetMapping("/nearby")
    public Result nearby(@RequestParam double x,
                         @RequestParam double y,
                         @RequestParam(defaultValue = "5") double distance) {
        return Result.ok(shopService.queryNearby(x, y, distance));
    }

    @PostMapping
    public Result save(@RequestBody Shop shop) {
        return Result.ok(shopService.saveShop(shop));
    }

    @PutMapping
    public Result update(@RequestBody Shop shop) {
        return shopService.updateShop(shop) ? Result.ok() : Result.fail("更新失败");
    }
}
