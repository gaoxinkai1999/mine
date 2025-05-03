package com.example.domain.batch.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 批次数据传输对象
 * 用于API返回，避免暴露JPA实体和懒加载问题
 */
@Data
public class BatchDto {
    private Integer id;
    private String batchNumber;
    private LocalDate productionDate;
    private LocalDate expirationDate;
    // 可以根据前端实际需要添加其他字段，例如：
    // private Integer productId;
    // private String productName;
}