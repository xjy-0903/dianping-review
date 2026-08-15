package com.dianping.controller;

import com.dianping.dto.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class IndexController {

    @GetMapping("/")
    public Result index() {
        return Result.ok(Map.of(
                "app", "本地生活点评服务平台(dianping-review)",
                "docs", "详见 README.md",
                "shop", "/api/shop/{id}",
                "nearby", "/api/shop/nearby?x=&y=&distance=",
                "seckill", "POST /api/voucher-order/seckill/{voucherId} (需 X-User-Id 请求头)",
                "sign", "POST /api/sign (需 X-User-Id 请求头)",
                "uv", "GET /api/stats/uv/{shopId}"
        ));
    }
}
