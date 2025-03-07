package com.example.domain.forecast.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品日销售数据模型
 * 存储商品在特定日期的销售信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDailySales {
    /**
     * 销售数量
     */
    private Integer quantity;
    
    /**
     * 销售额
     */
    private BigDecimal salesAmount;
    
    /**
     * 利润
     */
    private BigDecimal profit;
    
    /**
     * 基于原始数量和金额的构造函数
     * 
     * @param quantity 销售数量
     * @param sales 销售额
     * @param profit 利润
     */
    public ProductDailySales(int quantity, double sales, double profit) {
        this.quantity = quantity;
        this.salesAmount = BigDecimal.valueOf(sales);
        this.profit = BigDecimal.valueOf(profit);
    }
} 