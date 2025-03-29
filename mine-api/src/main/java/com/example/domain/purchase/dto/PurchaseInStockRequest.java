package com.example.domain.purchase.dto;

import com.example.domain.purchase.service.PurchaseService;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购入库请求DTO
 */
@Data
public class PurchaseInStockRequest {
    /**
     * 采购单ID
     */
    private Integer purchaseId;
    
    /**
     * 批次信息列表
     */
    private List<BatchInfo> batchInfoList;
    
    /**
     * 批次信息内部类
     */
    @Data
    public static class BatchInfo {
        /**
         * 采购明细ID
         */
        private Integer purchaseDetailId;
        
        /**
         * 生产日期
         */
        private LocalDate productionDate;
        
        /**
         * 过期日期
         */
        private LocalDate expirationDate;
    }
} 