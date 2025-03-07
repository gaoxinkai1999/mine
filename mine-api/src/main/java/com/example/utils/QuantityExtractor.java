package com.example.utils;

import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;

import java.math.BigDecimal;

/**
 * 提取销量的实现类
 */
public class QuantityExtractor implements DataExtractor {
    @Override
    public BigDecimal extract(ProductSalesInfoDTO productSalesInfoDTO) {
        return BigDecimal.valueOf(productSalesInfoDTO.getQuantity());
    }
}