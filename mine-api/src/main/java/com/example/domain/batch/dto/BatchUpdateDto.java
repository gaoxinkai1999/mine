package com.example.domain.batch.dto;

import lombok.Data;

import java.time.LocalDate; // 改为 LocalDate

@Data
public class BatchUpdateDto {
    private Integer id; // 必需，用于标识要更新的批次
    private String batchNumber; // 恢复 batchNumber，允许修改
    private LocalDate productionDate; // 改为 LocalDate
    private LocalDate expirationDate; // 改为 LocalDate
    private Boolean status;
    private String remark; // 新增 remark 字段
    // private Integer productId;  // 通常不需要通过 DTO 传递 productId 来更新批次
}