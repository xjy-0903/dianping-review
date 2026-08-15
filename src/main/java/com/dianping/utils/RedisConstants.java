package com.dianping.utils;

public final class RedisConstants {

    private RedisConstants() {
    }

    public static final String LOGIN_USER_KEY = "login:user:";
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String SECKILL_ORDER_SET_KEY = "seckill:ordered:";
    public static final String SECKILL_META_KEY = "seckill:voucher:";
    public static final String STREAM_ORDER_KEY = "stream:seckill:order";
    public static final String STREAM_ORDER_GROUP = "seckill-consumers";
    public static final String LOCK_ORDER_KEY = "lock:order:";
    public static final String SIGN_KEY = "sign:";
    public static final String UV_SHOP_KEY = "uv:shop:";
    public static final String GEO_SHOP_KEY = "geo:shops";
}
