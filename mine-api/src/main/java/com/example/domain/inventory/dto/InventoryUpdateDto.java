package com.example.domain.inventory.dto;

import lombok.Data;

@Data
public class InventoryUpdateDto {
    private Integer id;
    private Integer productId;  // 使用ID而不是嵌套对象
    private Integer batchId;    // 使用ID而不是嵌套对象
    private Integer quantity;
} 