package com.example.domain.forecast.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 窗口销售数据
 * 表示一个时间窗口内的销售汇总数据
 */
@Data
public class WindowSalesData {
    /**
     * 窗口起始日期
     */
    private LocalDate startDate;
    
    /**
     * 窗口结束日期
     */
    private LocalDate endDate;
    
    /**
     * 窗口内天数
     */
    private int dayCount;
    
    /**
     * 总销售额
     */
    private BigDecimal totalSales = BigDecimal.ZERO;
    
    /**
     * 总利润
     */
    private BigDecimal totalProfit = BigDecimal.ZERO;
    
    /**
     * 日均销售额
     */
    private double avgDailySales;
    
    /**
     * 下一天销售额
     */
    private BigDecimal nextDaySales;
    
    /**
     * 下一天利润
     */
    private BigDecimal nextDayProfit;
} 