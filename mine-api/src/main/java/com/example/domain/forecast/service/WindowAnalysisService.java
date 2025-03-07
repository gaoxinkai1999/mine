package com.example.domain.forecast.service;

import com.example.domain.forecast.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 滑动窗口分析服务
 * 提供数据窗口化和窗口相关分析功能
 */
@Service
@Slf4j
public class WindowAnalysisService {
    
    private static final int SCALE = 2; // 小数位数

    /**
     * 创建固定大小的滑动窗口数据
     * 从结束日期反向创建窗口，确保最新数据被完整包含
     *
     * @param dailySalesData 每日销售数据
     * @param windowSize 窗口大小（天数）
     * @return 窗口销售数据列表
     */
    public List<WindowSalesData> createSlidingWindows(List<DailySales> dailySalesData, int windowSize) {
        List<WindowSalesData> windows = new ArrayList<>();

        if (dailySalesData == null || dailySalesData.isEmpty()) {
            log.warn("销售数据为空，无法创建窗口");
            return windows;
        }

        // 确保数据按日期排序
        dailySalesData.sort(Comparator.comparing(DailySales::getDate));
        log.info("开始创建滑动窗口，数据范围：{} 至 {}", 
            dailySalesData.get(0).getDate(), 
            dailySalesData.get(dailySalesData.size() - 1).getDate());

        if (dailySalesData.size() < windowSize) {
            // 如果数据不足一个窗口，创建一个包含所有数据的窗口
            WindowSalesData window = new WindowSalesData();
            LocalDate startDate = dailySalesData.get(0).getDate();
            LocalDate endDate = dailySalesData.get(dailySalesData.size() - 1).getDate();
            window.setStartDate(startDate);
            window.setEndDate(endDate);

            BigDecimal totalSales = BigDecimal.ZERO;
            BigDecimal totalProfit = BigDecimal.ZERO;
            
            for (DailySales sale : dailySalesData) {
                totalSales = totalSales.add(BigDecimal.valueOf(sale.getSales()));
                totalProfit = totalProfit.add(BigDecimal.valueOf(sale.getProfit()));
            }
            
            window.setTotalSales(totalSales);
            window.setTotalProfit(totalProfit);
            window.setDayCount(dailySalesData.size());
            window.setAvgDailySales(totalSales.divide(
                BigDecimal.valueOf(window.getDayCount()), SCALE, RoundingMode.HALF_UP).doubleValue());
            
            windows.add(window);
            log.info("数据不足一个完整窗口，创建单一窗口: {} 至 {}, 包含 {} 天数据",
                startDate, endDate, dailySalesData.size());
            return windows;
        }

        // 临时存储窗口，之后会按时间顺序重新排列
        List<WindowSalesData> tempWindows = new ArrayList<>();

        // 从结束日期反向创建完整窗口
        int dataSize = dailySalesData.size();
        int remainingDays = dataSize;

        while (remainingDays >= windowSize) {
            WindowSalesData window = new WindowSalesData();
            int windowEndIndex = dataSize - (dataSize - remainingDays) - 1;
            int windowStartIndex = windowEndIndex - windowSize + 1;

            LocalDate startDate = dailySalesData.get(windowStartIndex).getDate();
            LocalDate endDate = dailySalesData.get(windowEndIndex).getDate();
            window.setStartDate(startDate);
            window.setEndDate(endDate);

            BigDecimal totalSales = BigDecimal.ZERO;
            BigDecimal totalProfit = BigDecimal.ZERO;
            
            for (int i = windowStartIndex; i <= windowEndIndex; i++) {
                totalSales = totalSales.add(BigDecimal.valueOf(dailySalesData.get(i).getSales()));
                totalProfit = totalProfit.add(BigDecimal.valueOf(dailySalesData.get(i).getProfit()));
            }
            
            window.setTotalSales(totalSales);
            window.setTotalProfit(totalProfit);
            window.setDayCount(windowSize);
            window.setAvgDailySales(totalSales.divide(
                BigDecimal.valueOf(window.getDayCount()), SCALE, RoundingMode.HALF_UP).doubleValue());

            tempWindows.add(window);
            remainingDays -= windowSize;
        }

        // 处理开始部分不足一个窗口的数据
        if (remainingDays > 0) {
            WindowSalesData partialWindow = new WindowSalesData();
            LocalDate startDate = dailySalesData.get(0).getDate();
            LocalDate endDate = dailySalesData.get(remainingDays - 1).getDate();
            partialWindow.setStartDate(startDate);
            partialWindow.setEndDate(endDate);

            BigDecimal totalSales = BigDecimal.ZERO;
            BigDecimal totalProfit = BigDecimal.ZERO;
            
            for (int i = 0; i < remainingDays; i++) {
                totalSales = totalSales.add(BigDecimal.valueOf(dailySalesData.get(i).getSales()));
                totalProfit = totalProfit.add(BigDecimal.valueOf(dailySalesData.get(i).getProfit()));
            }
            
            partialWindow.setTotalSales(totalSales);
            partialWindow.setTotalProfit(totalProfit);
            partialWindow.setDayCount(remainingDays);
            partialWindow.setAvgDailySales(totalSales.divide(
                BigDecimal.valueOf(partialWindow.getDayCount()), SCALE, RoundingMode.HALF_UP).doubleValue());

            tempWindows.add(partialWindow);
        }

        // 将窗口按时间顺序重新排列（从早到晚）
        tempWindows.sort(Comparator.comparing(WindowSalesData::getStartDate));

        // 添加到结果列表
        windows.addAll(tempWindows);

        log.info("成功创建 {} 个滑动窗口，第一个窗口: {} 至 {}, 最后一个窗口: {} 至 {}",
            windows.size(),
            windows.get(0).getStartDate(), windows.get(0).getEndDate(),
            windows.get(windows.size()-1).getStartDate(), windows.get(windows.size()-1).getEndDate());

        return windows;
    }
    
    /**
     * 创建产品销售窗口数据
     * 从结束日期反向创建窗口，确保最新数据被完整包含
     * 使用全局开始和结束日期，确保所有产品使用相同的窗口结构
     */
    public List<ProductWindowData> createProductWindows(
            List<ProductSales> productSales,
            int windowSize,
            LocalDate globalStartDate,
            LocalDate globalEndDate) {

        // 确保销售数据按日期排序
        productSales.sort(Comparator.comparing(ProductSales::getDate));

        // 创建窗口
        List<ProductWindowData> windows = new ArrayList<>();

        if (productSales.isEmpty()) {
            log.warn("产品销售数据为空，无法创建窗口");
            return windows;
        }

        String productName = productSales.get(0).getProductName();
        log.info("开始为产品「{}」创建销售窗口", productName);

        // 创建销售数据的日期映射，用于快速查找
        Map<LocalDate, ProductSales> salesByDate = productSales.stream()
                .collect(Collectors.toMap(
                    ProductSales::getDate,
                    ps -> ps,
                    (existing, replacement) -> {
                        // 合并同一天的销售数据
                        return new ProductSales(
                            existing.getDate(),
                            existing.getProductId(),
                            existing.getProductName(),
                            existing.getQuantity() + replacement.getQuantity(),
                            existing.getSales() + replacement.getSales(),
                            existing.getProfit() + replacement.getProfit()
                        );
                    }
                ));

        // 计算窗口的数量
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(globalStartDate, globalEndDate) + 1;
        int completeWindows = (int)(totalDays / windowSize);

        // 临时存储窗口，之后会按时间顺序重新排列
        List<ProductWindowData> tempWindows = new ArrayList<>();

        // 从结束日期反向创建完整窗口
        for (int i = 0; i < completeWindows; i++) {
            LocalDate windowEnd = globalEndDate.minusDays(i * windowSize);
            LocalDate windowStart = windowEnd.minusDays(windowSize - 1);

            // 确保不会超出全局数据范围
            if (windowStart.isBefore(globalStartDate)) {
                windowStart = globalStartDate;
            }

            ProductWindowData window = new ProductWindowData();
            window.setStartDate(windowStart);
            window.setEndDate(windowEnd);

            int daysWithData = 0;
            for (LocalDate date = windowStart; !date.isAfter(windowEnd); date = date.plusDays(1)) {
                if (salesByDate.containsKey(date)) {
                    ProductSales sale = salesByDate.get(date);
                    window.addSales(sale);
                    daysWithData++;
                }
            }

            tempWindows.add(window);
        }

        // 处理剩余的天数，确保所有数据都被包含
        LocalDate lastWindowStart = globalEndDate.minusDays((completeWindows * windowSize) - 1);
        if (lastWindowStart.isAfter(globalStartDate)) {
            ProductWindowData firstPartialWindow = new ProductWindowData();
            firstPartialWindow.setStartDate(globalStartDate);
            firstPartialWindow.setEndDate(lastWindowStart.minusDays(1));

            int daysWithData = 0;
            for (LocalDate date = globalStartDate; date.isBefore(lastWindowStart); date = date.plusDays(1)) {
                if (salesByDate.containsKey(date)) {
                    ProductSales sale = salesByDate.get(date);
                    firstPartialWindow.addSales(sale);
                    daysWithData++;
                }
            }

            // 只有当窗口有数据或强制使用全局窗口时才添加
            tempWindows.add(firstPartialWindow);
        }

        // 将窗口按时间顺序重新排列（从早到晚）
        tempWindows.sort(Comparator.comparing(ProductWindowData::getStartDate));

        // 重新编号窗口并添加到结果列表
        windows.addAll(tempWindows);

        if (windows.size() > 0) {
            log.info("产品「{}」创建了 {} 个窗口", productName, windows.size());
        }

        return windows;
    }

    /**
     * 计算窗口趋势可靠性指标
     * 返回0-1之间的值，越高表示趋势越可靠
     */
    public BigDecimal calculateWindowTrendReliability(List<WindowSalesData> windowDataList) {
        log.info("计算窗口趋势可靠性，窗口数量: {}", windowDataList.size());
        
        if (windowDataList == null || windowDataList.size() < 3) {
            log.warn("窗口数据不足，无法计算趋势可靠性");
            return BigDecimal.valueOf(0.3); // 默认低可靠性
        }

        try {
            // 1. 趋势一致性（最后3个窗口）
            double consistencyScore = 0.0;
            if (windowDataList.size() >= 3) {
                WindowSalesData last = windowDataList.get(windowDataList.size() - 1);
                WindowSalesData prev = windowDataList.get(windowDataList.size() - 2);
                WindowSalesData prev2 = windowDataList.get(windowDataList.size() - 3);

                boolean lastTrend = last.getTotalSales().compareTo(prev.getTotalSales()) > 0;
                boolean prevTrend = prev.getTotalSales().compareTo(prev2.getTotalSales()) > 0;

                consistencyScore = lastTrend == prevTrend ? 0.7 : 0.3;
            }

            // 2. 趋势强度（斜率）
            double trendStrength = 0.0;
            if (windowDataList.size() >= 2) {
                WindowSalesData last = windowDataList.get(windowDataList.size() - 1);
                WindowSalesData first = windowDataList.get(0);

                double lastSales = last.getTotalSales().doubleValue();
                double firstSales = first.getTotalSales().doubleValue();
                double periods = windowDataList.size() - 1;

                if (firstSales > 0 && periods > 0) {
                    double growthRate = Math.abs((lastSales - firstSales) / firstSales) / periods;
                    trendStrength = Math.min(1.0, growthRate * 5); // 标准化到0-1
                }
            }

            // 3. 数据波动性（线性度）
            double dataLinearity = 0.0;
            if (windowDataList.size() >= 3) {
                // 将窗口值转换为double数组
                double[] sales = new double[windowDataList.size()];
                for (int i = 0; i < windowDataList.size(); i++) {
                    sales[i] = windowDataList.get(i).getTotalSales().doubleValue();
                }

                // 计算线性拟合的R²值
                double r2 = calculateLinearFitR2(sales);
                dataLinearity = r2;
            }

            // 计算噪声水平（使用变异系数）
            double[] salesData = new double[windowDataList.size()];
            for (int i = 0; i < windowDataList.size(); i++) {
                salesData[i] = windowDataList.get(i).getTotalSales().doubleValue();
            }

            double mean = Arrays.stream(salesData).average().orElse(0);
            double variance = Arrays.stream(salesData)
                                   .map(d -> Math.pow(d - mean, 2))
                                   .average()
                                   .orElse(0);

            double cv = mean > 0 ? Math.sqrt(variance) / mean : 1.0;
            double noiseLevel = Math.max(0, 1 - Math.min(1, cv));

            // 综合得分
            double reliabilityScore = (
                consistencyScore * 0.3 + 
                trendStrength * 0.2 + 
                dataLinearity * 0.3 + 
                noiseLevel * 0.2
            );

            // 映射到较合理的0.2-0.9范围
            reliabilityScore = 0.2 + reliabilityScore * 0.7;
            
            BigDecimal result = BigDecimal.valueOf(reliabilityScore).setScale(4, RoundingMode.HALF_UP);
            
            log.info("趋势可靠性计算完成：一致性分数={}, A趋势强度={}, 数据线性度={}, 噪声水平={}, 最终可靠性分数={}",
                    BigDecimal.valueOf(consistencyScore).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(trendStrength).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(dataLinearity).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(noiseLevel).setScale(2, RoundingMode.HALF_UP),
                    result);
            
            return result;
        } catch (Exception e) {
            log.error("计算趋势可靠性时发生错误: {}", e.getMessage(), e);
            return BigDecimal.valueOf(0.3);
        }
    }

    /**
     * 计算线性拟合的R²值
     *
     * @param y 数据点
     * @return R²值
     */
    private double calculateLinearFitR2(double[] y) {
        // 使用最小二乘法计算线性拟合的斜率和截距
        int n = y.length;
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i;
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }

        // 计算斜率和截距
        double xMean = sumX / n;
        double yMean = sumY / n;
        
        // 避免除零错误
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            return 0;
        }
        
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = yMean - slope * xMean;

        // 计算拟合值
        double[] yFit = new double[n];
        for (int i = 0; i < n; i++) {
            yFit[i] = slope * x[i] + intercept;
        }

        // 计算R²
        double ssTotal = 0, ssResidual = 0;
        for (int i = 0; i < n; i++) {
            ssTotal += Math.pow(y[i] - yMean, 2);
            ssResidual += Math.pow(y[i] - yFit[i], 2);
        }

        double r2 = ssTotal > 0 ? 1 - (ssResidual / ssTotal) : 0;
        
        // 确保R²在0-1范围内
        return Math.max(0, Math.min(1, r2));
    }
} 