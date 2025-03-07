package com.example.domain.order.dto;

import com.example.domain.batch.entity.Batch;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchSaleRequest {
    private Batch batch;
    private Integer quantity;
} 