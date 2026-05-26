package com.minimall.minimall.common.util;

public class UserContext {

    public static ThreadLocal<Long>CURRENT_USER_ID=new ThreadLocal<Long>();
    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }
    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }
    public static void clear() {
        CURRENT_USER_ID.remove();
    }

}