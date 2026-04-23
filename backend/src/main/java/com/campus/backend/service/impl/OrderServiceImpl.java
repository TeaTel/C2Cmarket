package com.campus.backend.service.impl;

import com.campus.backend.dto.OrderCreateDTO;
import com.campus.backend.dto.OrderVO;
import com.campus.backend.dto.UserVO;
import com.campus.backend.entity.Order;
import com.campus.backend.entity.OrderItem;
import com.campus.backend.entity.Product;
import com.campus.backend.common.ErrorCode;
import com.campus.backend.exception.BusinessException;
import com.campus.backend.exception.NotFoundException;
import com.campus.backend.mapper.OrderMapper;
import com.campus.backend.mapper.ProductMapper;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务实现 (不含支付功能)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public OrderVO createOrder(OrderCreateDTO dto, Long buyerId) {
        // 验证卖家存在
        if (userMapper.selectById(dto.getSellerId()) == null) {
            throw new NotFoundException("卖家", dto.getSellerId());
        }
        // 不能自己给自己下单
        if (buyerId.equals(dto.getSellerId())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "不能向自己下单");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (OrderCreateDTO.OrderItemDTO itemDto : dto.getItems()) {
            Product product = productMapper.selectById(itemDto.getProductId());
            if (product == null) {
                throw new NotFoundException("商品", itemDto.getProductId());
            }
            if (!product.getSellerId().equals(dto.getSellerId())) {
                throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "商品不属于指定卖家");
            }
            if (product.getStatus() != 1) {
                throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE, "商品[" + product.getName() + "]当前不可购买");
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = new OrderItem();
            item.setProductId(itemDto.getProductId());
            item.setProductName(product.getName());
            item.setProductImage(product.getCoverImage());
            item.setPrice(product.getPrice());
            item.setQuantity(itemDto.getQuantity());
            item.setSubtotal(subtotal);
            items.add(item);
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(dto.getSellerId());
        order.setTotalAmount(totalAmount);
        order.setStatus("pending"); // 待确认
        order.setRemark(dto.getRemark());
        order.setAddress(dto.getAddress());
        orderMapper.insert(order);

        // 创建订单项
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderMapper.insertOrderItem(item);
        }

        log.info("订单创建成功: orderNo={}, buyer={}, amount={}", order.getOrderNo(), buyerId, totalAmount);
        return convertToVO(order);
    }

    @Override
    public OrderVO getOrderDetail(Long orderId, Long userId) {
        Order order = getOrderAndCheckAccess(orderId, userId);
        return convertToVO(order);
    }

    @Override
    public List<OrderVO> getBuyerOrders(Long buyerId) {
        return orderMapper.selectByBuyerId(buyerId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<OrderVO> getSellerOrders(Long sellerId) {
        return orderMapper.selectBySellerId(sellerId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, String status, Long operatorId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new NotFoundException("订单", orderId);
        }
        if (!order.getSellerId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有卖家可以操作此订单");
        }
        validateStatusTransition(order.getStatus(), status);

        LocalDateTime completedAt = "completed".equalsIgnoreCase(status) ? LocalDateTime.now() : null;
        orderMapper.updateStatus(orderId, status, completedAt);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long buyerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new NotFoundException("订单", orderId);
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有买家可以取消此订单");
        }
        if (!"pending".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "只能取消待确认的订单");
        }
        orderMapper.updateStatus(orderId, "cancelled", null);
    }

    @Override
    @Transactional
    public void confirmOrder(Long orderId, Long buyerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new NotFoundException("订单", orderId);
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有买家可以确认完成交易");
        }
        if (!"pending".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "当前状态无法确认完成");
        }
        orderMapper.updateStatus(orderId, "completed", LocalDateTime.now());
    }

    /** 验证状态流转合法性 */
    private void validateStatusTransition(String current, String target) {
        boolean valid = switch (target.toLowerCase()) {
            case "completed" -> "pending".equalsIgnoreCase(current);
            case "cancelled" -> "pending".equalsIgnoreCase(current);
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR,
                    "不允许从 [" + current + "] 变更为 [" + target + "]");
        }
    }

    /** 权限检查 */
    private Order getOrderAndCheckAccess(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new NotFoundException("订单", orderId);
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此订单");
        }
        return order;
    }

    /** 生成订单编号 */
    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /** Entity -> VO */
    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        // 订单项
        List<OrderItem> items = orderMapper.selectOrderItemsByOrderId(order.getId());
        List<OrderVO.OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderVO.OrderItemVO itemVO = new OrderVO.OrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList());
        vo.setItems(itemVOs);

        // 买家/卖家简要信息
        try {
            var buyer = userMapper.selectById(order.getBuyerId());
            if (buyer != null) {
                UserVO bvo = new UserVO();
                org.springframework.beans.BeanUtils.copyProperties(buyer, bvo);
                vo.setBuyerInfo(bvo);
            }
        } catch (Exception ignored) {}
        try {
            var seller = userMapper.selectById(order.getSellerId());
            if (seller != null) {
                UserVO svo = new UserVO();
                org.springframework.beans.BeanUtils.copyProperties(seller, svo);
                vo.setSellerInfo(svo);
            }
        } catch (Exception ignored) {}

        return vo;
    }
}
