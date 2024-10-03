package com.abdecd.moebackend.business.tokenLogin.interceptor;

import com.abdecd.moebackend.business.tokenLogin.common.TokenLoginConstant;
import com.abdecd.moebackend.business.tokenLogin.common.TokenLoginProp;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.business.tokenLogin.common.util.JwtUtil;
import com.abdecd.moebackend.business.tokenLogin.service.LoginBlackListService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.Map;

@Component
@Order(1)
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    @Resource
    private TokenLoginProp tokenLoginProp;
    @Resource
    private LoginBlackListService loginBlackListService;

    @Override
    public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) throws Exception {
        if (inTest()) return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        var canPass = false;
        PathPatternParser pathPatternParser = new PathPatternParser();
        for (String pattern : tokenLoginProp.getExcludePatterns()) {
            if (pathPatternParser.parse(pattern).matches(PathContainer.parsePath(request.getRequestURI()))) {
                canPass = true;
            }
        }
        log.info("uri: {}, query: {}", request.getRequestURI(), request.getQueryString());

        // 从请求头中获取令牌
        String token = request.getHeader(TokenLoginConstant.JWT_TOKEN_NAME);
        if (token == null || token.isEmpty()) {
            if (!canPass) return ret401(response);
            else return true;
        }
        // 校验令牌
        Map<String, String> claims = JwtUtil.decodeJWT(token);
        if (claims == null) {
            if (!canPass) return ret401(response);
            else return true;
        }
        Long userId = Long.valueOf(claims.get(TokenLoginConstant.K_USER_ID));
        String permission = claims.get(TokenLoginConstant.K_PERMISSION);
        long tokenTtlms = Long.parseLong(claims.get(TokenLoginConstant.K_EXPIRE));
        // 黑名单上的 token 无效
        if (loginBlackListService.checkInBlackList(userId, tokenTtlms)) return ret401(response);
        log.info("userId：{}, permission: {}", userId, permission);

        UserContext.setUserId(userId);
        UserContext.setPermission(permission);

        return true;
    }

    public boolean ret401(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("Content-Type", "text/plain");
        response.getWriter().println("401 Unauthorized");
        response.getWriter().close();
        return false;
    }

    public boolean inTest() {
        if (tokenLoginProp.getTest()) {
            UserContext.setUserId(1L);
            UserContext.setPermission("99");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void afterCompletion(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler, Exception ex) throws Exception {
        UserContext.clear();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
