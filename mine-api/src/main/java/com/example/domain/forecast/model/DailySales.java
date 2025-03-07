package com.example.domain.forecast.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 每日销售数据
 * 表示特定日期的销售总额和利润
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailySales {
    /**
     * 日期
     */
    private LocalDate date;
    
    /**
     * 销售额
     */
    private double sales;
    
    /**
     * 利润
     */
    private double profit;
} 