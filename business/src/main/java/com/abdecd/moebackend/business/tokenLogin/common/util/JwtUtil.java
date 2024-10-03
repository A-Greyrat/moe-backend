package com.abdecd.moebackend.business.tokenLogin.common.util;

import com.abdecd.moebackend.business.tokenLogin.common.TokenLoginConstant;
import com.abdecd.moebackend.business.tokenLogin.common.TokenLoginProp;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {
    private static Algorithm HMAC256 = Algorithm.HMAC256(UUID.randomUUID() + "");
    @Autowired
    public void setHMAC256(TokenLoginProp prop) {
        if (prop.getJwtSecretKey() != null)
            HMAC256 = Algorithm.HMAC256(prop.getJwtSecretKey());
    }

    public static String encodeJWT(String userId, String permission, int ttlSeconds) {
        var expiredDate = Calendar.getInstance();
        expiredDate.add(Calendar.SECOND, ttlSeconds);
        Map<String, Object> map = Map.of(
            TokenLoginConstant.K_USER_ID, userId,
            TokenLoginConstant.K_PERMISSION, permission
        );
        return JWT.create()
            .withClaim("claims", map)
            .withExpiresAt(expiredDate.getTime())
            .sign(HMAC256);
    }

    public static Map<String, String> decodeJWT(String token) {
        try {
            var verifiedToken = JWT
                .require(HMAC256)
                .build()
                .verify(token);
            var map = verifiedToken.getClaim("claims").asMap();
            return Map.of(
                TokenLoginConstant.K_USER_ID, map.get(TokenLoginConstant.K_USER_ID).toString(),
                TokenLoginConstant.K_PERMISSION, map.get(TokenLoginConstant.K_PERMISSION).toString(),
                TokenLoginConstant.K_EXPIRE, verifiedToken.getExpiresAt().getTime() + ""
            );
        } catch (Exception e) {
            return null;
        }
    }
}