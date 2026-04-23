package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 收藏控制器
 */
@RestController
@RequestMapping("/api/favorites")
@Tag(name = "收藏管理", description = "商品收藏功能")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{productId}")
    @Operation(summary = "收藏商品")
    public Result<Void> addFavorite(@PathVariable Long productId) {
        favoriteService.addFavorite(SecurityUtils.getCurrentUserId(), productId);
        return Result.success("收藏成功");
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "取消收藏")
    public Result<Void> removeFavorite(@PathVariable Long productId) {
        favoriteService.removeFavorite(SecurityUtils.getCurrentUserId(), productId);
        return Result.success("已取消收藏");
    }

    @GetMapping("/{productId}/check")
    @Operation(summary = "检查是否已收藏")
    public Result<Boolean> checkFavorited(@PathVariable Long productId) {
        return Result.success(favoriteService.isFavorited(SecurityUtils.getCurrentUserId(), productId));
    }
}
