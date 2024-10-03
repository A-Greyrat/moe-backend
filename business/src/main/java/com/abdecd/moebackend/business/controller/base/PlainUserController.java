package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.common.util.HttpCacheUtils;
import com.abdecd.moebackend.business.dao.entity.PlainUserDetail;
import com.abdecd.moebackend.business.pojo.dto.plainuser.UpdatePlainUserDTO;
import com.abdecd.moebackend.business.service.plainuser.PlainUserService;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "普通用户接口")
@RestController
@RequestMapping("/plain-user")
public class PlainUserController {
    @Autowired
    private PlainUserService plainUserService;

    @Operation(summary = "获取用户信息")
    @GetMapping("")
    public Result<PlainUserDetail> getUserInfo(
        Long uid,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        if (uid == null) uid = UserContext.getUserId();
        var vo = plainUserService.getPlainUserDetail(uid);
        if (HttpCacheUtils.tryUseCache(request, response, vo)) return null;
        return Result.success(vo);
    }

    @Operation(summary = "修改用户信息")
    @PostMapping("update")
    public Result<String> updateUserInfo(@Valid UpdatePlainUserDTO updatePlainUserDTO) {
        plainUserService.updatePlainUserDetail(updatePlainUserDTO);
        return Result.success();
    }
}

