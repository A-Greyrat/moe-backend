package com.abdecd.moebackend.business.tokenLogin.common;

import java.util.regex.Pattern;

public class TokenLoginConstant {
    public static final String K_USER_ID = "id";
    public static final String K_PERMISSION = "permission";
    public static final String K_EXPIRE = "exp";
    public static final String JWT_TOKEN_NAME = "token";

    public static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{6,20}$");

    /**
     * 在该值之前的token全部无效，单位ms
     */
    public static final String LOGIN_TOKEN_BLACKLIST = "learncloudspringsecurity:login_token_blacklist:";
}
