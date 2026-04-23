package com.campus.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 创建订单请求DTO (不含支付功能)
 */
@Data
public class OrderCreateDTO {

    @NotNull(message = "卖家ID不能为空")
    private Long sellerId;

    /** 商品列表 */
    @NotNull(message = "订单商品不能为空")
    private List<OrderItemDTO> items;

    /** 买家备注 */
    private String remark;

    /** 交易/收货地址 */
    private String address;

    /**
     * 订单项内部类
     */
    @Data
    public static class OrderItemDTO {

        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;
    }
}
