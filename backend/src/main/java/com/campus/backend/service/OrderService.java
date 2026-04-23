package com.campus.backend.service;

import com.campus.backend.dto.OrderCreateDTO;
import com.campus.backend.dto.OrderVO;

import java.util.List;

/**
 * 订单服务接口 (不含支付功能)
 */
public interface OrderService {

    /**
     * 创建订单
     */
    OrderVO createOrder(OrderCreateDTO createDTO, Long buyerId);

    /**
     * 订单详情
     */
    OrderVO getOrderDetail(Long orderId, Long userId);

    /**
     * 买家的订单列表
     */
    List<OrderVO> getBuyerOrders(Long buyerId);

    /**
     * 卖家的订单列表
     */
    List<OrderVO> getSellerOrders(Long sellerId);

    /**
     * 卖家更新订单状态 (确认/拒绝)
     */
    void updateOrderStatus(Long orderId, String status, Long operatorId);

    /**
     * 买家取消订单
     */
    void cancelOrder(Long orderId, Long buyerId);

    /**
     * 买家确认完成交易
     */
    void confirmOrder(Long orderId, Long buyerId);
}
