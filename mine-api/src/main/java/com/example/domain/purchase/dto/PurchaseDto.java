package com.example.domain.purchase.dto;

import com.example.domain.purchase.entity.PurchaseState;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购单DTO
 */
@Data
public class PurchaseDto {
    /**
     * 采购单ID
     */
    private Integer id;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 入库时间
     */
    private LocalDateTime inTime;
    
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 采购单状态
     */
    private PurchaseState state;
    
    /**
     * 采购明细
     */
    private List<PurchaseDetailDto> purchaseDetails;
} 