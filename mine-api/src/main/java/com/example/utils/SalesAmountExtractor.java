package com.example.utils;

import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;

import java.math.BigDecimal;

/**
 * 提取销售额的实现类
 */
public class SalesAmountExtractor implements DataExtractor {
    @Override
    public BigDecimal extract(ProductSalesInfoDTO productSalesInfoDTO) {
        return productSalesInfoDTO.getTotalSales();
    }

}