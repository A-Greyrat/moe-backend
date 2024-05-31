package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupWithDataVO;
import com.abdecd.moebackend.business.service.search.SearchService;
import com.abdecd.moebackend.common.result.PageVO;
import com.abdecd.moebackend.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "搜索")
@RestController
@RequestMapping("search")
public class SearchController {
    @Autowired
    private SearchService searchService;

    @Operation(summary = "搜索")
    @GetMapping("")
    public Result<PageVO<VideoGroupWithDataVO>> search(
            @RequestParam @NotBlank String q,
            @RequestParam(required = false) Byte type,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        return Result.success(searchService.search(q, type, page, pageSize));
    }

    @Operation(summary = "获得搜索建议")
    @GetMapping("suggestion")
    public Result<List<String>> getSuggestion(
            @RequestParam @NotBlank String q,
            @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(15) Integer num
    ) {
        return Result.success(searchService.getSearchSuggestions(q, num));
    }
}
