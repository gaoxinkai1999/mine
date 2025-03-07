package com.example.domain.forecast.controller;

import com.example.domain.forecast.model.ForecastResult;
import com.example.domain.forecast.service.SalesForecastService;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.domain.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 销售预测控制器
 * 提供销售预测相关的API接口
 */
@RestController
@RequestMapping("/forecast")
@Tag(name = "forecast", description = "销售预测相关接口")
@Slf4j
public class SalesForecastController {

    @Autowired
    private SalesForecastService forecastService;

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 获取销售预测
     *
     * @param days 预测天数，默认14天
     * @param historyDays 使用的历史天数，默认30天
     * @return 预测结果
     */
    @GetMapping("/sales")
    @Operation(summary = "获取销售预测", description = "根据历史销售数据预测未来销售情况")
    public ForecastResult getForecast(
            @Parameter(description = "预测天数", example = "14") 
            @RequestParam(value = "days", defaultValue = "14") Integer days,
            
            @Parameter(description = "使用的历史天数", example = "30") 
            @RequestParam(value = "historyDays", defaultValue = "30") Integer historyDays) {
        
        log.info("获取销售预测，预测天数: {}, 历史天数: {}", days, historyDays);
        
        try {
            // 确保参数在合理范围内
            int forecastDays = Math.min(Math.max(7, days), 60); // 限制在7-60天之间
            int dataHistoryDays = Math.min(Math.max(14, historyDays), 365); // 限制在14-365天之间
            
            // 计算日期范围
            LocalDate endDate = LocalDate.now().minusDays(1); // 截止到昨天
            LocalDate startDate = endDate.minusDays(dataHistoryDays - 1);
            
            // 获取历史销售数据
            Map<LocalDate, SalesStatisticsDTO> salesData = statisticsService.calculateDailyStatistics(startDate, endDate);
            
            if (salesData.isEmpty()) {
                log.warn("无历史销售数据可用于预测");
                throw new RuntimeException("无历史销售数据可用于预测");
            }
            
            // 生成预测
            ForecastResult forecast = forecastService.generateForecast(salesData);
            forecast.setForecastPeriodDays(forecastDays);
            
            log.info("销售预测生成成功，预测日期: {}, 预测准确性: {}", 
                    forecast.getForecastDate(), forecast.getAccuracyLevel());
            
            return forecast;
        } catch (Exception e) {
            log.error("生成销售预测时发生错误", e);
            throw new RuntimeException("生成销售预测失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取指定日期范围的销售预测
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 预测结果
     */
    @GetMapping("/sales/range")
    @Operation(summary = "获取指定日期范围的销售预测", description = "根据指定日期范围的历史销售数据进行预测")
    public ForecastResult getForecastByDateRange(
            @Parameter(description = "开始日期", example = "2023-01-01", required = true) 
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "结束日期", example = "2023-01-31", required = true) 
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("获取指定日期范围的销售预测，日期范围: {} 至 {}", startDate, endDate);
        
        try {
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("开始日期不能晚于结束日期");
            }
            
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            if (daysBetween < 14) {
                throw new IllegalArgumentException("日期范围至少需要14天才能进行有效预测");
            }
            
            if (daysBetween > 365) {
                throw new IllegalArgumentException("日期范围不能超过365天");
            }
            
            // 获取指定范围的历史销售数据
            Map<LocalDate, SalesStatisticsDTO> salesData = statisticsService.calculateDailyStatistics(startDate, endDate);
            
            if (salesData.isEmpty()) {
                log.warn("指定日期范围内无销售数据");
                throw new RuntimeException("指定日期范围内无销售数据");
            }
            
            // 生成预测
            ForecastResult forecast = forecastService.generateForecast(salesData);
            
            log.info("销售预测生成成功，预测日期: {}, 预测准确性: {}", 
                    forecast.getForecastDate(), forecast.getAccuracyLevel());
            
            return forecast;
        } catch (Exception e) {
            log.error("生成销售预测时发生错误", e);
            throw new RuntimeException("生成销售预测失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取产品销售预测
     *
     * @param productId 产品ID
     * @param days 预测天数，默认14天
     * @return 产品预测结果
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "获取产品销售预测", description = "预测特定产品的未来销售情况")
    public Map<String, Object> getProductForecast(
            @Parameter(description = "产品ID", required = true) 
            @PathVariable("productId") Integer productId,
            
            @Parameter(description = "预测天数", example = "14") 
            @RequestParam(value = "days", defaultValue = "14") Integer days) {
        
        log.info("获取产品销售预测，产品ID: {}, 预测天数: {}", productId, days);
        
        try {
            // 获取全局预测结果
            LocalDate endDate = LocalDate.now().minusDays(1);
            LocalDate startDate = endDate.minusDays(29); // 使用30天历史数据
            
            Map<LocalDate, SalesStatisticsDTO> salesData = statisticsService.calculateDailyStatistics(startDate, endDate);
            ForecastResult globalForecast = forecastService.generateForecast(salesData);
            
            // 检查产品是否有预测结果
            if (globalForecast.getProductForecasts() == null || 
                !globalForecast.getProductForecasts().containsKey(productId.toString())) {
                log.warn("产品 {} 无足够历史销售数据用于预测", productId);
                throw new RuntimeException("该产品无足够历史销售数据用于预测");
            }
            
            // 提取产品预测结果
            Map<String, Object> result = new HashMap<>();
            result.put("productForecast", globalForecast.getProductForecasts().get(productId.toString()));
            result.put("forecastDate", globalForecast.getForecastDate());
            result.put("forecastPeriodDays", days);
            result.put("forecastAccuracy", globalForecast.getForecastAccuracy());
            result.put("accuracyLevel", globalForecast.getAccuracyLevel());
            
            log.info("产品销售预测生成成功，产品ID: {}", productId);
            
            return result;
        } catch (Exception e) {
            log.error("生成产品销售预测时发生错误", e);
            throw new RuntimeException("生成产品销售预测失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取销售预测概要
     *
     * @return 预测概要信息
     */
    @GetMapping("/summary")
    @Operation(summary = "获取销售预测概要", description = "获取销售预测的概要信息，包含关键指标")
    public Map<String, Object> getForecastSummary() {
        log.info("获取销售预测概要");
        
        try {
            // 使用默认参数获取预测
            LocalDate endDate = LocalDate.now().minusDays(1);
            LocalDate startDate = endDate.minusDays(29);
            
            Map<LocalDate, SalesStatisticsDTO> salesData = statisticsService.calculateDailyStatistics(startDate, endDate);
            ForecastResult forecast = forecastService.generateForecast(salesData);
            
            // 提取关键信息
            Map<String, Object> summary = new HashMap<>();
            summary.put("forecastDate", forecast.getForecastDate());
            summary.put("salesForecast", forecast.getOverallSalesForecast());
            summary.put("profitForecast", forecast.getOverallProfitForecast());
            summary.put("trendDirection", forecast.getForecastTrendDirection());
            summary.put("accuracyLevel", forecast.getAccuracyLevel());
            summary.put("confidenceScore", forecast.getConfidenceScore());
            summary.put("topProducts", extractTopProducts(forecast, 5));
            
            log.info("销售预测概要生成成功");
            
            return summary;
        } catch (Exception e) {
            log.error("生成销售预测概要时发生错误", e);
            throw new RuntimeException("生成销售预测概要失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 提取预测中表现最好的产品
     *
     * @param forecast 预测结果
     * @param count 提取的产品数量
     * @return 表现最好的产品列表
     */
    private Map<String, Object> extractTopProducts(ForecastResult forecast, int count) {
        if (forecast.getProductForecasts() == null || forecast.getProductForecasts().isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("growingProducts", forecast.getProductForecasts().values().stream()
                .filter(p -> p.getGrowthRate().doubleValue() > 0)
                .sorted((p1, p2) -> p2.getGrowthRate().compareTo(p1.getGrowthRate()))
                .limit(count)
                .toArray());
                
        result.put("topSellers", forecast.getProductForecasts().values().stream()
                .sorted((p1, p2) -> p2.getSalesAmount().compareTo(p1.getSalesAmount()))
                .limit(count)
                .toArray());
                
        return result;
    }
} 