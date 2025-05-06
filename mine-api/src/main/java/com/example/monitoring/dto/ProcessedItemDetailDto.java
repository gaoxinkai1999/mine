package com.example.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用于展示已处理临期商品历史记录的 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedItemDetailDto {

    private Integer saleBatchDetailId;
    private String shopName;
    private String productName;
    private Integer orderId;
    private Integer orderDetailId;
    private Integer batchId;
    private LocalDate productionDate; // 批次生产日期
    private LocalDateTime processedAt;  // 处理时间

    // 注意：如果 Native Query 使用接口投影，则 getter 方法名需要与 SQL 列别名完全一致。
    // 如果使用构造函数表达式 (new com.example.dto.ProcessedItemDetailDto(...))，则构造函数参数顺序和类型需要匹配。
    // 为简单起见，这里使用 Lombok @AllArgsConstructor，并假设 Native Query 的列别名与字段名一致（或通过构造函数映射）。
}