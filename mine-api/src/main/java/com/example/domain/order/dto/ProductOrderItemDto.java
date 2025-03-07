package com.example.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品订单项DTO
 * 用于展示某个商品的所有销售订单项信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOrderItemDto {
    // 订单详情ID
    private Integer orderDetailId;
    
    // 订单ID
    private Integer orderId;
    
    // 订单创建时间
    private LocalDateTime orderTime;
    
    // 商品信息
    private Integer productId;
    private String productName;
    
    // 商家信息
    private Integer shopId;
    private String shopName;
    
    // 销售数量
    private Integer quantity;
    
    // 销售单价
    private BigDecimal salePrice;
    
    // 总销售金额
    private BigDecimal totalSalesAmount;
} 