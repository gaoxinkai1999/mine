package com.example.domain.forecast.service;

import com.example.domain.forecast.model.*;
import com.example.domain.product.service.ProductService;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 销售预测服务
 * 基于历史销售数据，使用简单而有效的统计方法预测未来销售额和利润
 * 采用固定天数滑动窗口方法，与日历月无关
 */
@Service
@Slf4j
public class SalesForecastService {

    private static final int FORECAST_DAYS = 30; // 预测未来14天
    private static final int SCALE = 2; // 小数位数
    private static final int MIN_DATA_POINTS = 365; // 最少需要的数据点数
    private static final int WINDOW_SIZE = 30; // 滑动窗口大小（天）

    @Autowired
    private ProductService productService; // 注入ProductService以获取在售商品

    @Autowired
    private DataConversionService dataConversionService;
    
    @Autowired
    private WindowAnalysisService windowAnalysisService;
    
    @Autowired
    private StatisticalCalculationService statisticalService;
    
    @Autowired
    private ProductForecastService productForecastService;

    /**
     * 生成销售预测
     *
     * @param salesDataMap 销售数据映射，键为日期，值为销售统计数据
     * @return 预测结果
     */
    public ForecastResult generateForecast(Map<LocalDate, SalesStatisticsDTO> salesDataMap) {
        log.info("开始生成销售预测，数据包含{}天", salesDataMap.size());

        if (salesDataMap == null || salesDataMap.isEmpty()) {
            log.warn("销售数据为空，无法生成预测");
            return dataConversionService.createEmptyForecast();
        }

        if (salesDataMap.size() < MIN_DATA_POINTS) {
            log.warn("销售数据不足，至少需要{}天数据才能生成有效预测", MIN_DATA_POINTS);
            return dataConversionService.createEmptyForecast();
        }

        // 1. 转换数据格式
        List<DailySales> dailySalesData = dataConversionService.convertToDailySales(salesDataMap);
        List<ProductSales> productSalesData = dataConversionService.convertToProductSales(salesDataMap);

        // 2. 生成预测
        return generateForecastFromData(dailySalesData, productSalesData);
    }

    /**
     * 从格式化的数据生成预测
     *
     * @param dailySalesData 日销售数据
     * @param productSalesData 商品销售数据
     * @return 预测结果
     */
    public ForecastResult generateForecastFromData(List<DailySales> dailySalesData, List<ProductSales> productSalesData) {
        log.info("从格式化数据生成预测，日销售数据: {} 天，商品销售数据: {} 个商品", 
                dailySalesData.size(), productSalesData.size());

        if (dailySalesData.isEmpty()) {
            log.warn("格式化后的销售数据为空，无法生成预测");
            return dataConversionService.createEmptyForecast();
        }

        try {
            // 1. 创建滑动窗口数据
            List<WindowSalesData> windowDataList = windowAnalysisService.createSlidingWindows(
                    dailySalesData, WINDOW_SIZE);

            if (windowDataList.isEmpty()) {
                log.warn("无法创建有效的滑动窗口，数据可能不足");
                return dataConversionService.createEmptyForecast();
            }
            
            log.info("创建了{}个滑动窗口数据", windowDataList.size());

            // 2. 提取历史销售数据用于统计计算
            double[] historicalSalesData = new double[dailySalesData.size()];
            for (int i = 0; i < dailySalesData.size(); i++) {
                historicalSalesData[i] = dailySalesData.get(i).getSales();
            }

            // 3. 计算不同预测方法的结果
            Map<String, BigDecimal> methodResults = new HashMap<>();
            double salesForecast;
            
            if (windowDataList.size() >= 4) {
                // 使用季节性调整的指数平滑
                double seasonalForecast = statisticalService.calculateSeasonalAdjustedForecast(
                        historicalSalesData);
                
                methodResults.put("seasonalForecast", 
                        BigDecimal.valueOf(seasonalForecast * FORECAST_DAYS)
                                .setScale(SCALE, RoundingMode.HALF_UP));
                salesForecast = seasonalForecast;
                
                log.info("使用季节性调整预测，基于{}个窗口数据", windowDataList.size());
            } else if (windowDataList.size() >= 2) {
                // 使用简单指数平滑
                double expSmoothingForecast = statisticalService.calculateSimpleExponentialSmoothing(
                        historicalSalesData, 0.3);
                
                methodResults.put("exponentialSmoothing", 
                        BigDecimal.valueOf(expSmoothingForecast * FORECAST_DAYS)
                                .setScale(SCALE, RoundingMode.HALF_UP));
                salesForecast = expSmoothingForecast;
                
                log.info("使用简单指数平滑预测，基于{}个窗口数据", windowDataList.size());
            } else {
                // 使用最近一个滑动窗口的平均值
                WindowSalesData lastWindow = windowDataList.get(windowDataList.size() - 1);
                double avgDailySales = lastWindow.getAvgDailySales();
                
                methodResults.put("average", 
                        BigDecimal.valueOf(avgDailySales * FORECAST_DAYS)
                                .setScale(SCALE, RoundingMode.HALF_UP));
                salesForecast = avgDailySales;
                
                log.info("使用最近窗口平均值预测，窗口为:{} 至 {}", 
                        lastWindow.getStartDate(), lastWindow.getEndDate());
            }

            // 4. 计算平均利润率和利润预测
            BigDecimal avgProfitRate = statisticalService.calculateAverageProfitRate(dailySalesData);
            BigDecimal totalSalesForecast = BigDecimal.valueOf(salesForecast * FORECAST_DAYS)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            BigDecimal totalProfitForecast = totalSalesForecast.multiply(avgProfitRate)
                    .setScale(SCALE, RoundingMode.HALF_UP);

            // 5. 计算销售波动性和预测区间
            BigDecimal salesVariability = statisticalService.calculateSimpleVariability(dailySalesData);
            BigDecimal lowerBound = totalSalesForecast.multiply(
                    BigDecimal.ONE.subtract(salesVariability))
                    .setScale(SCALE, RoundingMode.HALF_UP);
            BigDecimal upperBound = totalSalesForecast.multiply(
                    BigDecimal.ONE.add(salesVariability))
                    .setScale(SCALE, RoundingMode.HALF_UP);

            // 6. 获取当前在售商品ID并生成商品级预测
            Set<Integer> activeProductIds = productForecastService.getActiveProductIds();
            Map<String, ProductForecast> productForecasts = productForecastService.generateProductForecastsFromWindows(
                    productSalesData, WINDOW_SIZE, activeProductIds);
            
            log.info("生成了{}个商品的预测", productForecasts.size());

            // 7. 计算趋势方向和可靠性
            String historicalTrendDirection = "无";
            String forecastTrendDirection = "无";
            BigDecimal trendReliability = BigDecimal.ZERO;

            if (windowDataList.size() >= 2) {
                WindowSalesData lastWindow = windowDataList.get(windowDataList.size() - 1);
                WindowSalesData prevWindow = windowDataList.get(windowDataList.size() - 2);

                // 使用BigDecimal的compareTo方法比较值
                historicalTrendDirection = lastWindow.getTotalSales().compareTo(prevWindow.getTotalSales()) > 0 
                        ? "上升" : "下降";
                trendReliability = windowAnalysisService.calculateWindowTrendReliability(windowDataList);

                // 比较销售预测与最后一个窗口的销售额
                forecastTrendDirection = BigDecimal.valueOf(salesForecast)
                        .compareTo(lastWindow.getTotalSales().divide(BigDecimal.valueOf(lastWindow.getDayCount()), 2, RoundingMode.HALF_UP)) > 0 
                        ? "上升" : "下降";
                
                log.info("趋势分析：历史趋势={}, 预测趋势={}, 可靠性={}", 
                        historicalTrendDirection, forecastTrendDirection, trendReliability);
            }

            // 8. 计算预测准确性指标
            BigDecimal forecastAccuracy = statisticalService.calculateForecastAccuracy(
                    windowDataList, salesForecast, dailySalesData);
            String accuracyLevel = statisticalService.getForecastAccuracyLevel(forecastAccuracy);
            
            log.info("预测准确性评估: 准确度={}, 等级={}", forecastAccuracy, accuracyLevel);

            // 9. 计算其他评分指标
            BigDecimal dataQualityScore = BigDecimal.ONE.subtract(salesVariability)
                    .multiply(BigDecimal.valueOf(0.7))
                    .add(BigDecimal.valueOf(0.3))
                    .setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal reliabilityScore = trendReliability
                    .multiply(BigDecimal.valueOf(0.6))
                    .add(forecastAccuracy.multiply(BigDecimal.valueOf(0.4)))
                    .setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal confidenceScore = dataQualityScore
                    .multiply(BigDecimal.valueOf(0.4))
                    .add(reliabilityScore.multiply(BigDecimal.valueOf(0.6)))
                    .setScale(SCALE, RoundingMode.HALF_UP);

            // 10. 构建最终预测结果
            ForecastResult result = ForecastResult.builder()
                    .methodResults(methodResults)
                    .overallSalesForecast(totalSalesForecast)
                    .overallProfitForecast(totalProfitForecast)
                    .lowerBound(lowerBound)
                    .upperBound(upperBound)
                    .forecastAccuracy(forecastAccuracy)
                    .accuracyLevel(accuracyLevel)
                    .confidenceScore(confidenceScore)
                    .dataQualityScore(dataQualityScore)
                    .reliabilityScore(reliabilityScore)
                    .averageProfitRate(avgProfitRate)
                    .forecastVariability(salesVariability)
                    .historicalTrendDirection(historicalTrendDirection)
                    .forecastTrendDirection(forecastTrendDirection)
                    .trendReliability(trendReliability)
                    .productForecasts(productForecasts)
                    .forecastDate(LocalDate.now())
                    .forecastPeriodDays(FORECAST_DAYS)
                    .message("预测成功生成")
                    .build();

            log.info("预测完成。总预测销售额: {}, 总预测利润: {}, 预测准确度: {}", 
                    result.getOverallSalesForecast(), result.getOverallProfitForecast(), accuracyLevel);

            return result;
        } catch (Exception e) {
            log.error("生成预测过程中发生错误: {}", e.getMessage(), e);
            return dataConversionService.createEmptyForecast();
        }
    }
} 