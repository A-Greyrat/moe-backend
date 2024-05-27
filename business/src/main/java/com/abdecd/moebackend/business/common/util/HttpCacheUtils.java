package com.abdecd.moebackend.business.common.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;

public class HttpCacheUtils {
    /**
     * 使用 http 缓存并强制验证
     */
    public static boolean tryUseCache(HttpServletRequest request, HttpServletResponse response, LocalDateTime currentLocalDateTime) {
        if (currentLocalDateTime == null) return false;
        if (request.getHeader("If-None-Match") != null) {
            var clientLastTime = request.getHeader("If-None-Match").substring("W/".length());
            var clientLocalDateTime = LocalDateTime.parse(clientLastTime);
            if (!clientLocalDateTime.isBefore(currentLocalDateTime)) {
                response.setStatus(304);
                return true;
            }
        }
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("ETag", "W/" + currentLocalDateTime);
        return false;
    }

    /**
     * 使用 http 缓存并强制验证
     */
    public static boolean tryUseCache(HttpServletRequest request, HttpServletResponse response, Object willBeHashed) {
        if (willBeHashed == null) return false;
        var hashCode = willBeHashed.hashCode();
        if (request.getHeader("If-None-Match") != null) {
            var oldHash = request.getHeader("If-None-Match").substring("W/".length());
            if (oldHash.equals(hashCode + "")) {
                response.setStatus(304);
                return true;
            }
        }
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("ETag", "W/" + hashCode);
        return false;
    }
}
