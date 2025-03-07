package com.example.domain.forecast.service;

import com.example.domain.forecast.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统计计算服务
 * 提供各种预测相关的统计分析方法
 */
@Service
@Slf4j
public class StatisticalCalculationService {
    
    private static final int SCALE = 2; // 小数位数
    private static final double ALPHA = 0.3; // 指数平滑系数
    
    /**
     * 计算简单指数平滑预测
     * 适用于无明显趋势的平稳数据
     *
     * @param data 历史数据数组
     * @param alpha 平滑系数 (0-1)
     * @return 预测值
     */
    public double calculateSimpleExponentialSmoothing(double[] data, double alpha) {
        log.info("计算简单指数平滑预测，数据点数量: {}, 平滑系数: {}", data.length, alpha);
        
        if (data.length == 0) {
            log.warn("数据为空，无法进行指数平滑预测");
            return 0;
        }

        double smoothed = data[0];
        for (int i = 1; i < data.length; i++) {
            smoothed = alpha * data[i] + (1 - alpha) * smoothed;
        }

        log.info("指数平滑预测完成，最终平滑值: {}", smoothed);
        return smoothed;
    }
    
    /**
     * 计算季节性调整预测
     * 适用于有季节性波动的数据
     *
     * @param data 历史数据数组
     * @return 预测值
     */
    public double calculateSeasonalAdjustedForecast(double[] data) {
        log.info("计算季节性调整预测，数据点数量: {}", data.length);
        
        if (data.length < 4) {
            log.warn("数据点不足，无法分析季节性，将回退到简单指数平滑");
            return calculateSimpleExponentialSmoothing(data, ALPHA);
        }

        // 1. 计算季节性指数（假设4个数据点为一个周期）
        double[] seasonalIndices = calculateSeasonalIndices(data);
        
        log.info("季节性指数: {}", Arrays.toString(seasonalIndices));

        // 2. 计算去季节性的数据
        double[] deseasonalizedData = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            int season = i % 4;
            deseasonalizedData[i] = data[i] / seasonalIndices[season];
        }

        // 3. 对去季节性数据应用指数平滑
        double smoothed = calculateSimpleExponentialSmoothing(deseasonalizedData, ALPHA);
        
        // 4. 重新应用季节性
        int nextSeason = data.length % 4;
        double forecast = smoothed * seasonalIndices[nextSeason];
        
        log.info("季节性调整预测完成，去季节化平滑值: {}, 下一季节: {}, 最终预测值: {}", 
                smoothed, nextSeason, forecast);
                
        return forecast;
    }
    
    /**
     * 计算季节性指数
     * 用于识别数据中的周期性模式
     *
     * @param data 历史数据数组
     * @return 季节性指数数组
     */
    private double[] calculateSeasonalIndices(double[] data) {
        // 使用4个窗口作为一个季节周期
        double[] indices = new double[4];
        Arrays.fill(indices, 1.0); // 默认无季节性

        if (data.length < 4) {
            return indices;
        }

        // 计算每个季节的平均值
        double[] seasonalSum = new double[4];
        int[] seasonalCount = new int[4];

        for (int i = 0; i < data.length; i++) {
            int season = i % 4;
            seasonalSum[season] += data[i];
            seasonalCount[season]++;
        }

        double[] seasonalAvg = new double[4];
        for (int i = 0; i < 4; i++) {
            seasonalAvg[i] = seasonalCount[i] > 0 ? seasonalSum[i] / seasonalCount[i] : 0;
        }

        // 计算总体平均值
        double totalAvg = Arrays.stream(seasonalAvg).filter(v -> v > 0).average().orElse(1.0);

        // 计算季节性指数
        for (int i = 0; i < 4; i++) {
            indices[i] = seasonalAvg[i] > 0 ? seasonalAvg[i] / totalAvg : 1.0;
        }

        return indices;
    }
    
    /**
     * 计算简单变异性
     * 衡量销售数据的波动程度
     *
     * @param dailySales 日销售数据列表
     * @return 变异性系数 (0-1)
     */
    public BigDecimal calculateSimpleVariability(List<DailySales> dailySales) {
        log.info("计算销售波动性，数据点数量: {}", dailySales.size());
        
        if (dailySales.size() < 2) {
            log.warn("数据点不足，返回默认变异系数0.3");
            return BigDecimal.valueOf(0.3); // 默认变异系数
        }

        // 计算近期变异系数
        List<Double> recentSales = dailySales.stream()
                .skip(Math.max(0, dailySales.size() - 30)) // 最近30天
                .map(DailySales::getSales)
                .collect(Collectors.toList());

        double mean = recentSales.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        if (mean < 1e-10) {
            log.warn("平均销售额接近零，返回默认变异系数0.3");
            return BigDecimal.valueOf(0.3);
        }

        double variance = recentSales.stream()
                .mapToDouble(s -> Math.pow(s - mean, 2))
                .average()
                .orElse(0);

        double cv = Math.sqrt(variance) / mean;

        // 限制在合理范围内
        double adjustedCv = Math.max(0.2, Math.min(0.5, cv));
        
        log.info("销售波动性计算完成: {}，原始变异系数: {}, 调整后变异系数: {}", 
                BigDecimal.valueOf(adjustedCv), cv, adjustedCv);

        return BigDecimal.valueOf(adjustedCv).setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算平均利润率
     * 基于历史销售数据
     *
     * @param dailySales 日销售数据列表
     * @return 平均利润率
     */
    public BigDecimal calculateAverageProfitRate(List<DailySales> dailySales) {
        log.info("计算平均利润率，数据点数量: {}", dailySales.size());
        
        if (dailySales.isEmpty()) {
            log.warn("销售数据为空，返回默认利润率25%");
            return BigDecimal.valueOf(0.25); // 默认25%利润率
        }

        // 只考虑有销售的日期
        List<DailySales> validSales = dailySales.stream()
                .filter(sale -> sale.getSales() > 0)
                .collect(Collectors.toList());

        if (validSales.isEmpty()) {
            log.warn("没有有效销售数据，返回默认利润率25%");
            return BigDecimal.valueOf(0.25);
        }

        // 计算平均利润率
        double totalSales = validSales.stream().mapToDouble(DailySales::getSales).sum();
        double totalProfit = validSales.stream().mapToDouble(DailySales::getProfit).sum();
        
        BigDecimal profitRate = totalSales > 0 ?
                BigDecimal.valueOf(totalProfit / totalSales).setScale(4, RoundingMode.HALF_UP) :
                BigDecimal.valueOf(0.25);
                
        log.info("平均利润率计算完成: {}，基于总销售额: {}, 总利润: {}", 
                profitRate, totalSales, totalProfit);

        return profitRate;
    }
    
    /**
     * 计算预测准确度
     * 评估预测模型的准确性
     *
     * @param windowDataList 窗口数据列表
     * @param salesForecast 销售预测值
     * @param dailySalesData 实际销售数据
     * @return 预测准确度 (0-1)
     */
    public BigDecimal calculateForecastAccuracy(
            List<WindowSalesData> windowDataList, 
            double salesForecast, 
            List<DailySales> dailySalesData) {
        
        log.info("计算预测准确性指标，窗口数量: {}, 日销售数据量: {}", 
                windowDataList.size(), dailySalesData.size());
                
        if (windowDataList == null || windowDataList.size() < 2) {
            log.warn("窗口数据不足，返回默认准确性0.3");
            return BigDecimal.valueOf(0.3); // 数据不足，准确度较低
        }

        try {
            // 1. 数据稳定性评估 (使用变异系数)
            double[] salesData = new double[windowDataList.size()];
            for (int i = 0; i < windowDataList.size(); i++) {
                salesData[i] = windowDataList.get(i).getTotalSales().doubleValue();
            }

            double mean = Arrays.stream(salesData).average().orElse(0);
            if (mean < 1e-10) {
                log.warn("平均销售额接近零，返回默认准确性0.3");
                return BigDecimal.valueOf(0.3);
            }

            double variance = Arrays.stream(salesData)
                                   .map(d -> Math.pow(d - mean, 2))
                                   .average()
                                   .orElse(0);

            double cv = Math.sqrt(variance) / mean;
            double stability = Math.max(0, 1 - Math.min(1, cv * 2));

            // 2. 数据充足度评估
            double dataSufficiency = Math.min(1.0, windowDataList.size() / 6.0);

            // 3. 季节性强度评估
            double seasonality = 0.0;
            if (windowDataList.size() >= 4) {
                double[] seasonalIndices = calculateSeasonalIndices(salesData);

                // 计算季节性指数的变异程度
                double seasonalMean = Arrays.stream(seasonalIndices).average().orElse(1.0);
                double seasonalVariance = Arrays.stream(seasonalIndices)
                                               .map(d -> Math.pow(d - seasonalMean, 2))
                                               .average()
                                               .orElse(0);

                seasonality = Math.sqrt(seasonalVariance) / seasonalMean;
            }

            // 4. 最近波动性评估
            double recentVolatility = 0.0;
            if (windowDataList.size() >= 3) {
                WindowSalesData lastWindow = windowDataList.get(windowDataList.size() - 1);
                WindowSalesData prevWindow = windowDataList.get(windowDataList.size() - 2);

                double lastSales = lastWindow.getTotalSales().doubleValue();
                double prevSales = prevWindow.getTotalSales().doubleValue();

                if (prevSales > 0) {
                    recentVolatility = Math.min(1.0, Math.abs((lastSales - prevSales) / prevSales));
                }
            }

            // 5. 历史预测与实际值的偏差
            double dailyDataStability = 0.0;
            int forecastDays = 14; // 假设预测14天
            
            if (dailySalesData != null && dailySalesData.size() >= forecastDays * 2) {
                List<Double> recentSales = dailySalesData.stream()
                                                      .skip(Math.max(0, dailySalesData.size() - forecastDays * 2))
                                                      .map(DailySales::getSales)
                                                      .collect(Collectors.toList());

                // 计算相邻时间段的平均偏差
                double totalDeviation = 0.0;
                int deviationCount = 0;

                for (int i = 0; i < recentSales.size() - forecastDays; i++) {
                    double period1Avg = 0.0;
                    for (int j = 0; j < forecastDays; j++) {
                        period1Avg += recentSales.get(i + j);
                    }
                    period1Avg /= forecastDays;

                    double period2Avg = 0.0;
                    for (int j = 0; j < forecastDays; j++) {
                        if (i + forecastDays + j < recentSales.size()) {
                            period2Avg += recentSales.get(i + forecastDays + j);
                        }
                    }
                    period2Avg /= forecastDays;

                    if (period1Avg > 0) {
                        totalDeviation += Math.abs((period2Avg - period1Avg) / period1Avg);
                        deviationCount++;
                    }
                }

                if (deviationCount > 0) {
                    double avgDeviation = totalDeviation / deviationCount;
                    dailyDataStability = Math.max(0, 1 - Math.min(1, avgDeviation));
                }
            }

            // 综合计算预测准确度 (加权平均)
            double accuracy = (stability * 0.25 +
                              dataSufficiency * 0.20 +
                              (1 - seasonality) * 0.15 +
                              (1 - recentVolatility) * 0.20 +
                              dailyDataStability * 0.20);

            // 调整到合理范围 (0.2-0.95)
            accuracy = 0.2 + (accuracy * 0.75);
            
            BigDecimal accuracyResult = BigDecimal.valueOf(accuracy).setScale(4, RoundingMode.HALF_UP);

            log.info("预测准确性评估详情: 数据稳定性={}, 数据充足度={}, 季节性强度={}, 最近波动性={}, 日数据稳定性={}, 最终准确性={}",
                    BigDecimal.valueOf(stability).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(dataSufficiency).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(seasonality).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(recentVolatility).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(dailyDataStability).setScale(4, RoundingMode.HALF_UP),
                    accuracyResult);

            return accuracyResult;
        } catch (Exception e) {
            log.error("计算预测准确性时发生错误: {}", e.getMessage(), e);
            return BigDecimal.valueOf(0.3); // 发生错误时返回默认值
        }
    }
    
    /**
     * 获取预测准确度级别描述
     *
     * @param forecastAccuracy 预测准确度值
     * @return 准确度级别描述（高/中/低）
     */
    public String getForecastAccuracyLevel(BigDecimal forecastAccuracy) {
        double value = forecastAccuracy.doubleValue();
        
        String level;
        if (value >= 0.8) {
            level = "高度准确";
        } else if (value >= 0.6) {
            level = "较为准确";
        } else if (value >= 0.4) {
            level = "中等准确";
        } else {
            level = "参考性有限";
        }
        
        log.info("预测准确性等级: {}, 原始分数: {}", level, forecastAccuracy);
        return level;
    }
} 