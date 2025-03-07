package com.example.domain.product.dto.response;

import com.example.domain.product.dto.ProductStockDTO;
import lombok.Data;

/**
 * 商品库存响应, 包括商品ID、商品名称和商品库存信息
 * 用于封装商品库存信息，包括总库存和批次库存信息
 */
@Data
public class ProductStockResponse {
    private Integer id;
    private String name;
    private ProductStockDTO productStockDTO;
}
