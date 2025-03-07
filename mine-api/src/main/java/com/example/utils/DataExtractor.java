package com.example.utils;

import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;

import java.math.BigDecimal;

/**
 * 数据提取接口，用于从订单详情中提取特定数据（如销量、销售额、利润等）
 */
public interface DataExtractor {

    BigDecimal extract(ProductSalesInfoDTO productSalesInfoDTO);
}