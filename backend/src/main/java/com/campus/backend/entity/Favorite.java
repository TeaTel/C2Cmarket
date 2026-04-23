package com.campus.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品收藏实体类
 * 对应数据库 favorites 表
 */
@Data
public class Favorite {

    /** 收藏ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 收藏时间 */
    private LocalDateTime createdAt;
}
