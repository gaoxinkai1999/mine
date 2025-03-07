package com.example.domain.forecast.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品销售数据模型
 * 存储每个商品的销售历史
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSales {
    /**
     * 商品ID
     */
    private Integer productId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 按日期存储的销售数据
     */
    private Map<LocalDate, ProductDailySales> dailySales = new HashMap<>();
    
    /**
     * 兼容原有代码的构造函数
     */
    public ProductSales(LocalDate date, int productId, String productName, int quantity, double sales, double profit) {
        this.productId = productId;
        this.productName = productName;
        this.dailySales = new HashMap<>();
        
        ProductDailySales dailySale = new ProductDailySales();
        dailySale.setQuantity(quantity);
        dailySale.setSalesAmount(BigDecimal.valueOf(sales));
        dailySale.setProfit(BigDecimal.valueOf(profit));
        
        this.dailySales.put(date, dailySale);
    }
    
    /**
     * 兼容性方法 - 获取日期
     * 用于支持原有代码中按日期排序的功能
     */
    public LocalDate getDate() {
        if (dailySales == null || dailySales.isEmpty()) {
            return null;
        }
        return dailySales.keySet().iterator().next();
    }
    
    /**
     * 兼容性方法 - 获取销售数量
     */
    public int getQuantity() {
        if (dailySales == null || dailySales.isEmpty()) {
            return 0;
        }
        ProductDailySales firstDaySales = dailySales.values().iterator().next();
        return firstDaySales.getQuantity() != null ? firstDaySales.getQuantity() : 0;
    }
    
    /**
     * 兼容性方法 - 获取销售额
     */
    public double getSales() {
        if (dailySales == null || dailySales.isEmpty()) {
            return 0;
        }
        ProductDailySales firstDaySales = dailySales.values().iterator().next();
        return firstDaySales.getSalesAmount() != null ? firstDaySales.getSalesAmount().doubleValue() : 0;
    }
    
    /**
     * 兼容性方法 - 获取利润
     */
    public double getProfit() {
        if (dailySales == null || dailySales.isEmpty()) {
            return 0;
        }
        ProductDailySales firstDaySales = dailySales.values().iterator().next();
        return firstDaySales.getProfit() != null ? firstDaySales.getProfit().doubleValue() : 0;
    }
} 