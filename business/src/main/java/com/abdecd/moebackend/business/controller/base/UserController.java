package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.pojo.dto.user.*;
import com.abdecd.moebackend.business.service.common.CommonService;
import com.abdecd.moebackend.business.service.user.UserService;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.result.Result;
import com.abdecd.tokenlogin.common.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户接口")
@Slf4j
@RestController
@RequestMapping("user")
public class UserController {
    @Resource
    UserService userService;
    @Resource
    CommonService commonService;

    @Operation(summary = "用户注册", description = "data字段返回用户token")
    @PostMapping("/signup")
    public Result<String> signup(@RequestBody @Valid SignUpDTO signUpDTO) {
        var user = userService.signup(signUpDTO);
        var token = userService.generateUserToken(user);
        return Result.success(token);
    }

    @Operation(summary = "用户登录", description = "data字段返回用户token")
    @PostMapping("/login")
    public Result<String> login(@RequestBody @Valid LoginDTO loginDTO) {
        commonService.verifyCaptcha(loginDTO.getVerifyCodeId(), loginDTO.getCaptcha());
        var user = userService.login(loginDTO);
        if (user == null) {
            return Result.error(MessageConstant.LOGIN_PASSWORD_ERROR);
        }
        var token = userService.generateUserToken(user);
        return Result.success(token);
    }

    @Operation(summary = "用户使用邮箱登录", description = "data字段返回用户token")
    @PostMapping("/login-by-email")
    public Result<String> loginByEmail(@RequestBody @Valid LoginByEmailDTO loginByEmailDTO) {
        var user = userService.loginByEmail(loginByEmailDTO);
        if (user == null) {
            return Result.error(MessageConstant.LOGIN_FAIL);
        }
        var token = userService.generateUserToken(user);
        return Result.success(token);
    }

    @Operation(summary = "用户续登", description = "data字段返回用户token")
    @GetMapping("/login/refresh")
    public Result<String> loginRefresh() {
        var token = userService.refreshUserToken();
        return Result.success(token);
    }

    @Operation(summary = "忘记密码")
    @PostMapping("/forget-password")
    public Result<String> forgetPassword(@RequestBody @Valid ResetPwdDTO resetPwdDTO) {
        userService.forgetPassword(resetPwdDTO);
        return Result.success();
    }

    @Operation(summary = "用户注销账号")
    @PostMapping("/delete-account")
    public Result<String> deleteAccount(@RequestBody @Valid DeleteAccountDTO deleteAccountDTO) {
        userService.deleteAccount(UserContext.getUserId(), deleteAccountDTO.getVerifyCode());
        return Result.success();
    }

    @Operation(summary = "用户修改邮箱")
    @PostMapping("/change-email")
    public Result<String> changeEmail(@RequestBody @Valid ChangeEmailDTO changeEmailDTO) {
        userService.changeEmail(UserContext.getUserId(), changeEmailDTO);
        return Result.success();
    }
}
