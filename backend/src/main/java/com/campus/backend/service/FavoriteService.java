package com.campus.backend.service;

/**
 * 收藏服务接口
 */
public interface FavoriteService {

    /**
     * 收藏商品
     */
    void addFavorite(Long userId, Long productId);

    /**
     * 取消收藏
     */
    void removeFavorite(Long userId, Long productId);

    /**
     * 检查是否已收藏
     */
    boolean isFavorited(Long userId, Long productId);

    /**
     * 获取收藏数
     */
    int countByProductId(Long productId);
}
