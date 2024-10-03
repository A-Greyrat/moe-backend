package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.lib.RateLimiter;
import com.abdecd.moebackend.business.pojo.dto.user.*;
import com.abdecd.moebackend.business.service.LoginService;
import com.abdecd.moebackend.business.service.common.CommonService;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.business.tokenLogin.service.TokenLoginService;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.RedisConstant;
import com.abdecd.moebackend.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Tag(name = "用户接口")
@Slf4j
@RestController
@RequestMapping("user")
public class UserController {
    @Autowired
    LoginService loginService;
    @Autowired
    TokenLoginService tokenLoginService;
    @Autowired
    CommonService commonService;
    @Autowired
    RateLimiter rateLimiter;

    @Operation(summary = "用户注册", description = "data字段返回用户token")
    @PostMapping("/signup")
    public Result<String> signup(@RequestBody @Valid SignUpDTO signUpDTO) {
        // 验证邮箱
        commonService.verifyEmail(signUpDTO.getEmail(), signUpDTO.getVerifyCode());
        var user = loginService.signup(signUpDTO);
        var token = tokenLoginService.generateUserToken(user.getId() + "", user.getPermission());
        return Result.success(token);
    }

    @Operation(summary = "用户登录", description = "data字段返回用户token")
    @PostMapping("/login")
    public Result<String> login(@RequestBody @Valid LoginDTO loginDTO, HttpServletRequest request) {
        if (rateLimiter.isRateLimited(
            RedisConstant.LIMIT_LOGIN + loginDTO.getUsername() + ":" + request.getHeader("X-Real-IP"),
            2,
            1,
            TimeUnit.SECONDS)
        ) throw new BaseException(MessageConstant.RATE_LIMIT);
        // 验证验证码
        commonService.verifyCaptcha(loginDTO.getVerifyCodeId(), loginDTO.getCaptcha());
        var user = loginService.login(loginDTO);
        if (user == null) {
            return Result.error(MessageConstant.LOGIN_PASSWORD_ERROR);
        }
        var token = tokenLoginService.generateUserToken(user.getId() + "", user.getPermission());
        return Result.success(token);
    }

    @Operation(summary = "用户使用邮箱登录", description = "data字段返回用户token")
    @PostMapping("/login-by-email")
    public Result<String> loginByEmail(@RequestBody @Valid LoginByEmailDTO loginByEmailDTO) {
        // 验证邮箱
        commonService.verifyEmail(loginByEmailDTO.getEmail(), loginByEmailDTO.getVerifyCode());
        var user = loginService.loginByEmail(loginByEmailDTO);
        if (user == null) {
            return Result.error(MessageConstant.LOGIN_FAIL);
        }
        var token = tokenLoginService.generateUserToken(user.getId() + "", user.getPermission());
        return Result.success(token);
    }

    @Operation(summary = "用户续登", description = "data字段返回用户token")
    @GetMapping("/login/refresh")
    public Result<String> loginRefresh() {
        var token = tokenLoginService.refreshUserToken();
        return Result.success(token);
    }

    @Operation(summary = "忘记密码")
    @PostMapping("/forget-password")
    public Result<String> forgetPassword(@RequestBody @Valid ResetPwdDTO resetPwdDTO) {
        // 验证邮箱
        commonService.verifyEmail(resetPwdDTO.getEmail(), resetPwdDTO.getVerifyCode());
        loginService.forgetPassword(resetPwdDTO);
        return Result.success();
    }

    @Operation(summary = "用户注销账号")
    @PostMapping("/delete-account")
    public Result<String> deleteAccount(@RequestBody @Valid DeleteAccountDTO deleteAccountDTO) {
        // 方法内验证邮箱
        loginService.deleteAccount(UserContext.getUserId(), deleteAccountDTO.getVerifyCode());
        return Result.success();
    }

    @Operation(summary = "用户修改邮箱")
    @PostMapping("/change-email")
    public Result<String> changeEmail(@RequestBody @Valid ChangeEmailDTO changeEmailDTO) {
        // 验证邮箱
        commonService.verifyEmail(changeEmailDTO.getNewEmail(), changeEmailDTO.getVerifyCode());
        loginService.changeEmail(UserContext.getUserId(), changeEmailDTO);
        return Result.success();
    }
}
