package com.abdecd.moebackend.business.controller.backstage;

import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.pojo.dto.recommend.AddCarouselDTO;
import com.abdecd.moebackend.business.pojo.dto.recommend.DeleteCarouselDTO;
import com.abdecd.moebackend.business.pojo.dto.recommend.SetCarouselDTO;
import com.abdecd.moebackend.business.pojo.vo.videogroup.VideoGroupWithDataVO;
import com.abdecd.moebackend.business.service.RecommendService;
import com.abdecd.moebackend.business.tokenLogin.aspect.RequirePermission;
import com.abdecd.moebackend.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequirePermission(value = "99", exception = BaseException.class)
@Tag(name = "推荐接口")
@RestController
@RequestMapping("/backstage/video-group")
public class RecommendControllerBack {
    @Autowired
    private RecommendService recommendService;

    @Operation(summary = "获取轮播列表")
    @GetMapping("carousel")
    public Result<List<VideoGroupWithDataVO>> getCarousel() {
        return Result.success(recommendService.getCarousel());
    }

    @Operation(summary = "设置轮播列表")
    @PostMapping("carousel")
    public Result<String> setCarousel(@RequestBody @Valid SetCarouselDTO setCarouselDTO) {
        recommendService.setCarouselIds(setCarouselDTO.getIds());
        return Result.success();
    }

    @Operation(summary = "添加轮播列表")
    @PostMapping("carousel/add")
    public Result<String> addCarousel(@RequestBody @Valid AddCarouselDTO dto) {
        recommendService.addCarouselIds(dto.getIndex(), dto.getIds());
        return Result.success();
    }

    @Operation(summary = "删除轮播列表")
    @PostMapping("carousel/delete")
    public Result<String> deleteCarousel(@RequestBody @Valid DeleteCarouselDTO dto) {
        recommendService.deleteCarouselIds(dto.getIds());
        return Result.success();
    }
}
