package com.abdecd.moebackend.business.controller.backstage;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.pojo.dto.plainuser.BanUserDTO;
import com.abdecd.moebackend.business.pojo.dto.plainuser.UpdateUserPermissionDTO;
import com.abdecd.moebackend.business.pojo.vo.plainuser.AllVO;
import com.abdecd.moebackend.business.service.PlainUserManage.PlainUserManageService;
import com.abdecd.moebackend.common.result.Result;
import com.abdecd.tokenlogin.aspect.RequirePermission;
import com.abdecd.tokenlogin.common.context.UserContext;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/backstage/plain-user")
public class PlainUserManageController {

    @Autowired
    private PlainUserManageService userService;

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "列出所有用户")
    @GetMapping("/list")
    public Result<Object> listUsers(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") @Max(200) int pageSize) {

        Page<AllVO> userPage = userService.listUsers(id, name, status, page, pageSize);
        return Result.success(Map.of(
                "total", userPage.getTotal(),
                "records", userPage.getRecords()
        ));
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "封禁/解封用户")
    @PostMapping("/ban")
    public Result<String> banUser(@RequestBody @Valid BanUserDTO banUserDTO) {
        if (Objects.equals(banUserDTO.getId(), UserContext.getUserId())) throw new BaseException("不需要对自己操作");
        boolean success = userService.banUser(banUserDTO);
        if (success) {
            return Result.success("ok");
        } else {
            return Result.error("用户处理失败");
        }
    }

    @RequirePermission(value = "99", exception = BaseException.class)
    @Operation(summary = "修改用户权限")
    @PostMapping("/update-permission")
    public Result<String> updatePermission(@RequestBody @Valid UpdateUserPermissionDTO dto) {
        if (Objects.equals(dto.getId(), UserContext.getUserId())) throw new BaseException("不需要对自己操作");
        userService.updatePermission(dto.getId(), dto.getPermission());
        return Result.success();
    }
}
