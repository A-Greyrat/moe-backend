package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.common.util.HttpCacheUtils;
import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.dao.mapper.VideoGroupMapper;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupWithDataVO;
import com.abdecd.moebackend.business.service.RecommendService;
import com.abdecd.moebackend.business.service.videogroup.VideoGroupServiceBase;
import com.abdecd.moebackend.common.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "推荐接口")
@RestController
@RequestMapping("/video-group")
public class RecommendController {
    @Autowired
    private RecommendService recommendService;
    @Autowired
    private VideoGroupMapper videoGroupMapper;
    @Autowired
    private VideoGroupServiceBase videoGroupServiceBase;

    @Operation(summary = "获取轮播列表")
    @GetMapping("carousel")
    public Result<List<VideoGroupWithDataVO>> getCarousel(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var vo = recommendService.getCarousel();
        if (HttpCacheUtils.tryUseCache(request, response, vo)) return null;
        return Result.success(vo);
    }

    @Operation(summary = "获取推荐列表")
    @GetMapping("recommend")
    public Result<List<VideoGroupWithDataVO>> getRecommendList(@RequestParam @Min(1) @Max(20) int num) {
        return Result.success(recommendService.getRecommend(num));
    }

    @Operation(summary = "获取相关视频列表")
    @GetMapping("related")
    public DeferredResult<Result<List<VideoGroupWithDataVO>>> getRelatedList(
            @RequestParam Long id,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int num
    ) {
        var df = new DeferredResult<Result<List<VideoGroupWithDataVO>>>(800L);
        df.onTimeout(() -> {
            var ids = videoGroupMapper.selectList(new LambdaQueryWrapper<VideoGroup>()
                    .select(VideoGroup::getId)
                    .ne(VideoGroup::getId, id)
                    .last("order by RAND() limit " + num)
            );
            List<VideoGroupWithDataVO> result;
            if (ids.isEmpty()) {
                result = new ArrayList<>();
            } else {
                result = new ArrayList<>(ids.stream()
                    .map(idd -> videoGroupServiceBase.getVideoGroupWithData(idd.getId()))
                    .toList()
                );
            }
            df.setResult(Result.success(result));
        });
        Thread.ofVirtual().start(() -> df.setResult(Result.success(recommendService.getRelated(id, num))));
        return df;
    }
}
