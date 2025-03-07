package com.example.domain.forecast.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 预测结果
 * 包含整体销售预测和具体商品预测信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastResult {
    /**
     * 各种预测方法的结果
     */
    private Map<String, BigDecimal> methodResults;
    
    /**
     * 整体销售预测
     */
    private BigDecimal overallSalesForecast;
    
    /**
     * 整体利润预测
     */
    private BigDecimal overallProfitForecast;
    
    /**
     * 预测下限
     */
    private BigDecimal lowerBound;
    
    /**
     * 预测上限
     */
    private BigDecimal upperBound;
    
    /**
     * 商品预测结果，键为商品名称
     */
    private Map<String, ProductForecast> productForecasts;
    
    /**
     * 历史趋势方向
     */
    private String historicalTrendDirection;
    
    /**
     * 预测趋势方向
     */
    private String forecastTrendDirection;
    
    /**
     * 趋势可靠性
     */
    private BigDecimal trendReliability;
    
    /**
     * 预测准确度
     */
    private BigDecimal forecastAccuracy;
    
    /**
     * 准确度等级描述
     */
    private String accuracyLevel;
    
    /**
     * 预测波动性
     */
    private BigDecimal forecastVariability;
    
    /**
     * 数据质量评分
     */
    private BigDecimal dataQualityScore;
    
    /**
     * 可靠性评分
     */
    private BigDecimal reliabilityScore;
    
    /**
     * 置信度评分
     */
    private BigDecimal confidenceScore;
    
    /**
     * 平均利润率
     */
    private BigDecimal averageProfitRate;
    
    /**
     * 预测日期
     */
    private LocalDate forecastDate;
    
    /**
     * 预测天数
     */
    private Integer forecastPeriodDays;
    
    /**
     * 预测消息
     */
    private String message;
} 