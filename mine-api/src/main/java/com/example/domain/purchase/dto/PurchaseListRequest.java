package com.example.domain.purchase.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PurchaseListRequest {
    // 采购单状态
    private String state;
    
    // 创建时间范围
    private LocalDateTime createTimeStart;
    private LocalDateTime createTimeEnd;

    
    // 分页参数
    private Integer page = 0;
    private Integer size = 10;
} 