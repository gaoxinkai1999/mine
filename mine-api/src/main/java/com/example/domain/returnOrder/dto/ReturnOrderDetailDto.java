package com.example.domain.returnOrder.dto;

import com.example.domain.returnOrder.entity.ReturnType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退货订单详情数据传输对象
 */
@Data
public class ReturnOrderDetailDto {
    /**
     * 退货订单详情ID
     */
    private Integer id;

    /**
     * 商品信息
     */
    private ProductSimpleDto product;

    /**
     * 退货金额
     */
    private BigDecimal amount;

    /**
     * 退货类型
     */
    private ReturnType type;
    
    /**
     * 退货数量
     */
    private Integer quantity;

    @Data
    public static class ProductSimpleDto {
        private Integer id;
        private String name;
    }
}