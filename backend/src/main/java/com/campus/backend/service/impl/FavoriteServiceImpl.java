package com.campus.backend.service.impl;

import com.campus.backend.exception.NotFoundException;
import com.campus.backend.mapper.FavoriteMapper;
import com.campus.backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 收藏服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;

    @Override
    @Transactional
    public void addFavorite(Long userId, Long productId) {
        if (!favoriteMapper.exists(userId, productId)) {
            com.campus.backend.entity.Favorite fav = new com.campus.backend.entity.Favorite();
            fav.setUserId(userId);
            fav.setProductId(productId);
            favoriteMapper.insert(fav);
            log.info("收藏成功: userId={}, productId={}", userId, productId);
        }
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long productId) {
        int rows = favoriteMapper.delete(userId, productId);
        if (rows > 0) {
            log.info("取消收藏: userId={}, productId={}", userId, productId);
        }
    }

    @Override
    public boolean isFavorited(Long userId, Long productId) {
        return favoriteMapper.exists(userId, productId);
    }

    @Override
    public int countByProductId(Long productId) {
        return favoriteMapper.countByProductId(productId);
    }
}
