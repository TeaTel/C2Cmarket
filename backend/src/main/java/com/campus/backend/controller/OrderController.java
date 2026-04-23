package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.dto.OrderCreateDTO;
import com.campus.backend.dto.OrderVO;
import com.campus.backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单管理控制器 (不含支付功能)
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@Tag(name = "订单管理", description = "创建订单、查看历史、确认完成")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "创建订单")
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.createOrder(dto, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(id, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/buyer")
    @Operation(summary = "我买到的订单")
    public Result<List<OrderVO>> getBuyerOrders() {
        return Result.success(orderService.getBuyerOrders(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/seller")
    @Operation(summary = "我卖出的订单")
    public Result<List<OrderVO>> getSellerOrders() {
        return Result.success(orderService.getSellerOrders(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "卖家: 更新订单状态 (completed确认完成 / cancelled取消)")
    public Result<Void> updateOrderStatus(@PathVariable Long id,
                                           @RequestParam String status) {
        orderService.updateOrderStatus(id, status, SecurityUtils.getCurrentUserId());
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "买家: 取消订单")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id, SecurityUtils.getCurrentUserId());
        return Result.success("取消成功");
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "买家: 确认交易完成")
    public Result<Void> confirmOrder(@PathVariable Long id) {
        orderService.confirmOrder(id, SecurityUtils.getCurrentUserId());
        return Result.success("已确认完成");
    }
}
