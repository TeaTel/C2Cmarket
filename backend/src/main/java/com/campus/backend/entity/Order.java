package com.campus.backend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类 (不含支付功能)
 * 对应数据库 orders 表
 */
@Data
public class Order {

    /** 订单ID */
    private Long id;

    /** 订单编号 (唯一业务编号) */
    private String orderNo;

    /** 买家用户ID */
    private Long buyerId;

    /** 卖家用户ID */
    private Long sellerId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /**
     * 订单状态:
     * pending-待确认, cancelled-已取消,
     * completed-已完成, refunded-已退款
     */
    private String status;

    /** 买家备注 */
    private String remark;

    /** 交易/收货地址 */
    private String address;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;
}
