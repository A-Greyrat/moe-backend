package com.abdecd.moebackend.business.controller.backstage;

import com.abdecd.moebackend.business.common.exception.BaseException;
import com.abdecd.moebackend.business.pojo.dto.notice.NoticeDTO;
import com.abdecd.moebackend.business.pojo.dto.notice.NoticeUpdateDTO;
import com.abdecd.moebackend.business.pojo.vo.notice.Notice;
import com.abdecd.moebackend.business.service.NoticeService;
import com.abdecd.moebackend.common.result.Result;
import com.abdecd.tokenlogin.aspect.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequirePermission(value = "99", exception = BaseException.class)
@Tag(name = "公告接口")
@RestController
@RequestMapping("/backstage/notice")
public class NoticeControllerBack {
    @Autowired
    private NoticeService noticeService;

    @Operation(summary = "获取公告列表")
    @GetMapping("list")
    public Result<List<Notice>> getNoticeList() {
        return Result.success(noticeService.getNoticeList());
    }

    @Operation(summary = "添加公告")
    @PostMapping("add")
    public Result<String> addNotice(@RequestBody @Valid NoticeDTO dto) {
        if (dto.getIndex() != null) {
            noticeService.addNotice(dto.getIndex(), dto.toEntity());
        } else noticeService.addNoticeAtFirst(dto.toEntity());
        return Result.success();
    }

    public record DeleteNoticeDTO(@NotNull Integer index) {
    }

    @Operation(summary = "删除公告")
    @PostMapping("delete")
    public Result<String> deleteNotice(@RequestBody @Valid DeleteNoticeDTO dto) {
        noticeService.deleteNotice(dto.index);
        return Result.success();
    }

    @Operation(summary = "更新公告")
    @PostMapping("update")
    public Result<String> updateNotice(@RequestBody @Valid NoticeUpdateDTO dto) {
        noticeService.updateNotice(dto.getIndex(), dto.toEntity());
        return Result.success();
    }

}
