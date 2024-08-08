package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.pojo.vo.notice.Notice;
import com.abdecd.moebackend.business.service.NoticeService;
import com.abdecd.moebackend.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "公告接口")
@RestController
@RequestMapping("notice")
public class NoticeController {
    @Autowired
    private NoticeService noticeService;

    @Operation(summary = "获取公告列表")
    @GetMapping("list")
    public Result<List<Notice>> getNoticeList(@RequestParam(required = false) String recentHash) {
        var list = noticeService.getNoticeList();
        if (recentHash != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getHash().equals(recentHash)) {
                    list = list.subList(0, i);
                    break;
                }
            }
        }
        return Result.success(list);
    }
}
