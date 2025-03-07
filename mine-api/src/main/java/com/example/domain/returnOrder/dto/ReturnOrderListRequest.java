package com.example.domain.returnOrder.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 退货订单列表请求DTO
 * 用于查询退货订单列表，支持分页和筛选
 */
@Data
public class ReturnOrderListRequest {
    
    private Integer shopId;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // 分页信息
    private Integer page = 0;
    private Integer size = 10;
} 