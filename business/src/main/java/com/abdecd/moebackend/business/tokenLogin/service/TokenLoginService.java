package com.abdecd.moebackend.business.tokenLogin.service;

import com.abdecd.moebackend.business.tokenLogin.common.TokenLoginConstant;
import com.abdecd.moebackend.business.tokenLogin.common.TokenLoginProp;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.business.tokenLogin.common.util.JwtUtil;
import com.abdecd.moebackend.business.tokenLogin.common.util.PwdUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@Slf4j
public class TokenLoginService {
    @Resource
    private TokenLoginProp tokenLoginProp;
    @Resource
    private LoginBlackListService loginBlackListService;

    /**
     * 密码解密 并校验密码强度
     * @param password 前端传过来的密码
     * @return 解密后的密码
     * @throws Exception 密码强度不够等
     */
    public String convertPwd(String password) throws Exception {
        password = PwdUtil.decryptPwd(password);
        if (!TokenLoginConstant.PASSWORD_PATTERN.matcher(password).find()) {
            throw new Exception("密码格式错误");
        }
        return password;
    }

    public String generateUserToken(String userId, String permission) {
        return JwtUtil.encodeJWT(userId, permission, tokenLoginProp.getJwtTtlSeconds());
    }

    public String refreshUserToken() {
        return JwtUtil.encodeJWT(UserContext.getUserId() + "", UserContext.getPermission(), tokenLoginProp.getJwtTtlSeconds());
    }

    public void forceLogout(Long userId) {
        loginBlackListService.forceLogout(userId);
    }

    /**
     * 返回Base64编码的公钥 为pem格式去掉开头和结尾的-----BEGIN PUBLIC KEY-----和-----END PUBLIC KEY-----
     */
    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(PwdUtil.publicKey.getEncoded());
    }
}
