package com.example.utils;

import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;

import java.math.BigDecimal;

/**
 * 提取利润的实现类
 */
public class ProfitExtractor implements DataExtractor {
    @Override
    public BigDecimal extract(ProductSalesInfoDTO productSalesInfoDTO) {

        return productSalesInfoDTO.getTotalProfit();
    }
}