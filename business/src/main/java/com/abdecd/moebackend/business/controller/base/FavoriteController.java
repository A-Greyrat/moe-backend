package com.abdecd.moebackend.business.controller.base;

import com.abdecd.moebackend.business.dao.entity.VideoGroup;
import com.abdecd.moebackend.business.pojo.dto.favorite.AddFavoritesDTO;
import com.abdecd.moebackend.business.pojo.dto.favorite.DeleteFavoritesDTO;
import com.abdecd.moebackend.business.pojo.vo.favorite.FavoriteVO;
import com.abdecd.moebackend.business.service.BangumiIndexService;
import com.abdecd.moebackend.business.service.FavoriteService;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.common.result.PageVO;
import com.abdecd.moebackend.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Tag(name = "收藏接口")
@RestController
@RequestMapping("/plain-user/favorites")
public class FavoriteController {
    @Autowired
    private FavoriteService favoriteService;
    @Autowired
    private BangumiIndexService bangumiIndexService;

//    @Operation(summary = "获取用户收藏列表")
//    @GetMapping("")
//    public Result<PageVO<VideoGroupWithDataVO>> getFavorites(
//            @RequestParam(defaultValue = "1") Integer page,
//            @RequestParam(defaultValue = "10") Integer pageSize
//    ) {
//        return Result.success(favoriteService.get(UserContext.getUserId(), page, pageSize));
//    }

    @Operation(summary = "分类获取用户收藏列表")
    @GetMapping("")
    public Result<PageVO<FavoriteVO>> getFavorites(
            @RequestParam @Min(0) @Max(1) Byte type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") @Max(200) Integer pageSize
    ) {
        if (Objects.equals(type, VideoGroup.Type.PLAIN_VIDEO_GROUP)) {
            return Result.success(favoriteService.getPlainFavorite(UserContext.getUserId(), page, pageSize));
        } else if (Objects.equals(type, VideoGroup.Type.ANIME_VIDEO_GROUP)) {
            return Result.success(favoriteService.getBangumiFavorite(UserContext.getUserId(), page, pageSize));
        }
        return null;
    }

    @Operation(summary = "获取用户是否收藏特定视频组")
    @GetMapping("contains")
    public Result<Boolean> getExists(@RequestParam Long videoGroupId) {
        return Result.success(favoriteService.isUserFavorite(UserContext.getUserId(), videoGroupId));
    }

    @Operation(summary = "删除用户收藏")
    @PostMapping("delete")
    public Result<String> deleteFavorites(@RequestBody @Valid DeleteFavoritesDTO deleteFavoritesDTO) {
        favoriteService.delete(UserContext.getUserId(), deleteFavoritesDTO.getVideoGroupIds());
        for (var id : deleteFavoritesDTO.getVideoGroupIds())
            bangumiIndexService.decreaseFavorite(id);
        return Result.success();
    }

    @Operation(summary = "添加用户收藏")
    @PostMapping("add")
    public Result<String> addFavorites(@RequestBody @Valid AddFavoritesDTO addFavoritesDTO) {
        favoriteService.add(UserContext.getUserId(), addFavoritesDTO.getVideoGroupId());
        bangumiIndexService.recordFavorite(addFavoritesDTO.getVideoGroupId());
        return Result.success();
    }
}
