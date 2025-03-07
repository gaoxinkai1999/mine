package com.example.domain.returnOrder.dto;

import com.example.domain.returnOrder.entity.ReturnType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建退货订单请求
 */
@Data
public class ReturnOrderRequest {
    /**
     * 商店ID
     */
    @NotNull(message = "商店ID不能为空")
    @Positive(message = "商店ID必须为正数")
    private Integer shopId;
    
    /**
     * 退货订单详情列表
     */
    @NotEmpty(message = "退货订单详情不能为空")
    @Valid
    private List<ReturnOrderDetailRequest> details;
    
    /**
     * 退货订单详情请求
     */
    @Data
    public static class ReturnOrderDetailRequest {
        /**
         * 商品ID
         */
        @NotNull(message = "商品ID不能为空")
        @Positive(message = "商品ID必须为正数")
        private Integer productId;
        
        /**
         * 退货金额
         */
        @NotNull(message = "退货金额不能为空")
        @Positive(message = "退货金额必须为正数")
        private BigDecimal amount;
        
        /**
         * 退货类型
         */
        @NotNull(message = "退货类型不能为空")
        private ReturnType type;
        
        /**
         * 退货数量 (当退货类型为"退货退款"时必须大于0)
         */
        private Integer quantity;
    }
}