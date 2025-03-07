package com.example.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 产品订单列表请求DTO
 * 用于查询特定产品的所有销售订单项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductOrderListRequest {
    // 产品ID
    private Integer productId;
    
    // 时间范围筛选
    private LocalDate startDate;
    private LocalDate endDate;
    
    // 分页信息
    private int page = 0;
    private int size = 10;
} 