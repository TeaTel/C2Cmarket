package com.campus.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情VO (返回前端)
 */
@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal totalAmount;
    private String status;
    private String statusText;
    private String remark;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    /** 买家信息 */
    private UserVO buyerInfo;

    /** 卖家信息 */
    private UserVO sellerInfo;

    /** 订单项列表 */
    private List<OrderItemVO> items;

    /** 获取状态文本 */
    public String getStatusText() {
        if (status == null) return "未知";
        return switch (status.toLowerCase()) {
            case "pending" -> "待确认";
            case "cancelled" -> "已取消";
            case "completed" -> "已完成";
            case "refunded" -> "已退款";
            default -> "未知";
        };
    }

    /**
     * 订单项VO内部类
     */
    @Data
    public static class OrderItemVO {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;
        /** 关联的商品详情 */
        private ProductVO productInfo;
    }
}
