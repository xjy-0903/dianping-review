package com.dianping.utils;

public final class UserHolder {

    private static final ThreadLocal<Long> TL = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void save(Long userId) {
        TL.set(userId);
    }

    public static Long get() {
        return TL.get();
    }

    public static void remove() {
        TL.remove();
    }
}
