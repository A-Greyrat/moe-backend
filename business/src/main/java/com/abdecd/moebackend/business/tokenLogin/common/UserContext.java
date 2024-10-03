package com.abdecd.moebackend.business.tokenLogin.common;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.Data;

public class UserContext {
    private static final ThreadLocal<UserContextHolder> userContext = TransmittableThreadLocal.withInitial(UserContextHolder::new);

    public static void setUserId(Long userId) {
        userContext.get().setUserId(userId);
    }

    public static Long getUserId() {
        return userContext.get().getUserId();
    }

    public static void setPermission(String permission) {
        userContext.get().setPermission(permission);
    }

    public static String getPermission() {
        return userContext.get().getPermission();
    }

    public static void clear() {
        userContext.remove();
    }

    @Data
    public static class UserContextHolder {
        private Long userId;
        private String permission;
    }
}
