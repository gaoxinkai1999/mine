package com.example.domain.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购单明细DTO
 */
@Data
public class PurchaseDetailDto {
    /**
     * 明细ID
     */
    private Integer id;
    
    /**
     * 商品ID
     */
    private Integer productId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 采购数量
     */
    private Integer quantity;
    
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 批次ID
     */
    private Integer batchId;
} 