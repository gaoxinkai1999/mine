package com.example.monitoring.dto; // Updated package

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用于表示临期批次信息的 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearExpiryBatchInfo {
    private String shopName; // 商家名称
    private String productName; // 商品名称
    private String batchNumber; // 批次号
    private LocalDate productionDate; // 生产日期
    private Long daysSinceProduction; // 生产距今天数
    private Integer thresholdDays; // 商品设置的临期阈值（天）
    private LocalDateTime latestRelevantOrderTime; // 该商家包含批次管理商品的最新订单时间
    private Integer shopId; // 商家ID
    private Integer productId; // 商品ID
    private Integer batchId; // 批次ID
}