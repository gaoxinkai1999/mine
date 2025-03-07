package com.example.domain.forecast.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品预测结果
 * 表示单个商品的销售预测数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductForecast {
    /**
     * 商品ID
     */
    private Integer productId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 商品类别
     */
    private String category;
    
    /**
     * 上月销量
     */
    private Integer lastMonthSales;
    
    /**
     * 预测销量
     */
    private Integer predictedSales;
    
    /**
     * 增长率
     */
    private BigDecimal growthRate;
    
    /**
     * 预测销售额
     */
    private BigDecimal salesAmount;
    
    /**
     * 预测利润额
     */
    private BigDecimal profitAmount;
} 