package com.campus.backend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项实体类
 * 对应数据库 order_items 表
 */
@Data
public class OrderItem {

    /** 订单项ID */
    private Long id;

    /** 所属订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 商品名称 (下单时快照) */
    private String productName;

    /** 商品图片 (下单时快照) */
    private String productImage;

    /** 下单时单价 (快照) */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 小计金额 */
    private BigDecimal subtotal;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
