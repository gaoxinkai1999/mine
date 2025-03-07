package com.example.domain.forecast.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商品窗口销售数据
 * 表示一个时间窗口内特定商品的销售汇总数据
 */
@Data
@NoArgsConstructor
public class ProductWindowData {
    /**
     * 窗口起始日期
     */
    private LocalDate startDate;
    
    /**
     * 窗口结束日期
     */
    private LocalDate endDate;
    
    /**
     * 商品ID
     */
    private Integer productId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 总销售数量
     */
    private int totalQuantity;
    
    /**
     * 总销售额
     */
    private BigDecimal totalSales = BigDecimal.ZERO;
    
    /**
     * 总利润
     */
    private BigDecimal totalProfit = BigDecimal.ZERO;
    
    /**
     * 添加单日销售数据到窗口
     */
    public void addSales(ProductSales sale) {
        this.productId = sale.getProductId();
        this.productName = sale.getProductName();
        
        this.totalQuantity += sale.getQuantity();
        this.totalSales = this.totalSales.add(BigDecimal.valueOf(sale.getSales()));
        this.totalProfit = this.totalProfit.add(BigDecimal.valueOf(sale.getProfit()));
    }
} 