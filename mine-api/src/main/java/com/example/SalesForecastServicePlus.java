package com.example;

import com.example.domain.product.entity.Product;
import com.example.domain.product.service.ProductService;
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.query.ProductQuery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 销售预测服务
 * 基于历史销售数据，使用简单而有效的统计方法预测未来销售额和利润
 * 采用固定天数滑动窗口方法，与日历月无关
 */
@Service
public class SalesForecastServicePlus {

    private static final Logger logger = LoggerFactory.getLogger(SalesForecastServicePlus.class);
    private static final int FORECAST_DAYS = 30; // 预测未来14天
    private static final int SCALE = 2; // 小数位数

    @Autowired
    private ProductService productService; // 注入ProductService以获取在售商品

    /**
     * 生成销售预测
     *
     * @param salesDataMap 销售数据映射，键为日期，值为销售统计数据
     * @return 预测结果
     */
    public ForecastResult generateForecast(Map<LocalDate, SalesStatisticsDTO> salesDataMap) {
        logger.debug("开始生成销售预测，数据包含{}天", salesDataMap.size());

        if (salesDataMap == null || salesDataMap.isEmpty()) {
            logger.warn("销售数据为空，无法生成预测");
            return createEmptyForecast();
        }

        // 将原始数据转换为预测模型对象
        List<DailySales> dailySalesData = convertToDailySales(salesDataMap);
        List<ProductSales> productSalesData = convertToProductSales(salesDataMap);

        return generateForecastFromData(dailySalesData, productSalesData);
    }

    /**
     * 转换为每日销售数据列表
     */
    private List<DailySales> convertToDailySales(Map<LocalDate, SalesStatisticsDTO> salesDataMap) {
        List<DailySales> dailySales = new ArrayList<>();

        for (Map.Entry<LocalDate, SalesStatisticsDTO> entry : salesDataMap.entrySet()) {
            LocalDate date = entry.getKey();
            SalesStatisticsDTO dto = entry.getValue();

            if (dto.getTotalSales() != null && dto.getTotalProfit() != null) {
                dailySales.add(new DailySales(
                        date,
                        dto.getTotalSales().doubleValue(),
                        dto.getTotalProfit().doubleValue()
                ));
            }
        }

        // 按日期排序
        dailySales.sort(Comparator.comparing(DailySales::getDate));
        return dailySales;
    }

    /**
     * 转换为产品销售数据列表
     */
    private List<ProductSales> convertToProductSales(Map<LocalDate, SalesStatisticsDTO> salesDataMap) {
        List<ProductSales> productSales = new ArrayList<>();

        for (Map.Entry<LocalDate, SalesStatisticsDTO> entry : salesDataMap.entrySet()) {
            LocalDate date = entry.getKey();
            SalesStatisticsDTO dto = entry.getValue();

            if (dto.getProductSalesInfoDTOS() != null) {
                for (ProductSalesInfoDTO productSaleDTO : dto.getProductSalesInfoDTOS()) {
                    if (productSaleDTO.getQuantity() > 0) {
                        productSales.add(new ProductSales(
                                date,
                                productSaleDTO.getProductId(),
                                productSaleDTO.getProductName(),
                                productSaleDTO.getQuantity(),
                                productSaleDTO.getTotalSales().doubleValue(),
                                productSaleDTO.getTotalProfit().doubleValue()
                        ));
                    }
                }
            }
        }

        // 按日期排序
        productSales.sort(Comparator.comparing(ProductSales::getDate));
        return productSales;
    }

    /**
     * 基于销售数据生成预测，使用固定天数滑动窗口方法
     */
    public ForecastResult generateForecastFromData(List<DailySales> dailySalesData, List<ProductSales> productSalesData) {
        logger.debug("开始基于{}条日销售数据和{}条产品销售数据生成预测",
                dailySalesData.size(), productSalesData.size());

        if (dailySalesData == null || dailySalesData.isEmpty()) {
            logger.warn("销售数据为空，无法生成预测");
            return createEmptyForecast();
        }

        try {
            // 使用固定天数滑动窗口处理数据
            List<WindowSalesData> windowDataList = createSlidingWindows(dailySalesData, FORECAST_DAYS);
            logger.debug("创建了{}个滑动窗口数据", windowDataList.size());

            // 选择合适的预测方法
            double salesForecast;
            Map<String, BigDecimal> methodResults = new HashMap<>();

            if (windowDataList.size() >= 4) { // 至少有4个窗口才考虑季节性
                // 使用季节性调整的指数平滑
                double[] windowSalesData = windowDataList.stream()
                        .mapToDouble(WindowSalesData::getAvgDailySales)
                        .toArray();

                double seasonalForecast = calculateSeasonalAdjustedForecast(windowSalesData) * FORECAST_DAYS;
                methodResults.put("seasonalForecast", BigDecimal.valueOf(seasonalForecast).setScale(SCALE, RoundingMode.HALF_UP));
                salesForecast = seasonalForecast;

                logger.debug("使用季节性调整预测，基于{}个窗口数据", windowDataList.size());
            } else if (windowDataList.size() >= 2) {
                // 使用简单指数平滑
                double[] windowSalesData = windowDataList.stream()
                        .mapToDouble(WindowSalesData::getAvgDailySales)
                        .toArray();

                double alpha = 0.3;
                double esForecast = calculateSimpleExponentialSmoothing(windowSalesData, alpha) * FORECAST_DAYS;
                methodResults.put("exponentialSmoothing", BigDecimal.valueOf(esForecast).setScale(SCALE, RoundingMode.HALF_UP));
                salesForecast = esForecast;

                logger.debug("使用简单指数平滑预测，基于{}个窗口数据", windowDataList.size());
            } else {
                // 窗口数据太少，使用最近窗口的平均值
                WindowSalesData lastWindow = windowDataList.get(windowDataList.size() - 1);
                double avgForecast = lastWindow.getAvgDailySales() * FORECAST_DAYS;
                methodResults.put("average", BigDecimal.valueOf(avgForecast).setScale(SCALE, RoundingMode.HALF_UP));
                salesForecast = avgForecast;

                logger.debug("使用最近窗口平均值预测，窗口起始日期={}, 结束日期={}",
                    lastWindow.getStartDate(), lastWindow.getEndDate());
            }

            // 计算利润预测
            BigDecimal avgProfitRate = calculateAverageProfitRate(dailySalesData);
            BigDecimal profitForecast = BigDecimal.valueOf(salesForecast)
                    .multiply(avgProfitRate)
                    .setScale(SCALE, RoundingMode.HALF_UP);

            // 计算简单预测区间
            double variability = calculateSimpleVariability(dailySalesData);
            double lowerBound = salesForecast * (1 - variability);
            double upperBound = salesForecast * (1 + variability);

            // 获取当前在售的商品ID列表
            Set<Integer> activeProductIds = getActiveProductIds();
            logger.debug("当前在售商品数量: {}", activeProductIds.size());

            // 只预测在售商品
            Map<String, ProductForecast> productForecasts = generateProductForecastsFromWindows(
                    productSalesData,
                    FORECAST_DAYS,
                    activeProductIds
            );

            // 计算趋势方向和可靠性
            String historicalTrendDirection = "无";
            String forecastTrendDirection = "无";
            BigDecimal trendReliability = BigDecimal.ZERO;

            if (windowDataList.size() >= 2) {
                WindowSalesData lastWindow = windowDataList.get(windowDataList.size() - 1);
                WindowSalesData prevWindow = windowDataList.get(windowDataList.size() - 2);

                // 历史趋势方向（前一窗口与最新窗口的对比）
                historicalTrendDirection = lastWindow.getTotalSales() > prevWindow.getTotalSales() ? "上升" : "下降";
                trendReliability = calculateWindowTrendReliability(windowDataList);

                logger.debug("历史趋势分析：对比前一窗口({}至{})与最新窗口({}至{}), 趋势方向={}, 可靠性={}",
                    prevWindow.getStartDate(), prevWindow.getEndDate(),
                    lastWindow.getStartDate(), lastWindow.getEndDate(),
                    historicalTrendDirection, trendReliability);

                // 预测趋势方向（最新窗口与预测窗口的对比）
                double lastWindowTotalSales = lastWindow.getTotalSales();
                forecastTrendDirection = salesForecast > lastWindowTotalSales ? "上升" : "下降";

                logger.debug("预测趋势分析：对比最新窗口({}至{})销售额{}与预测窗口销售额{}, 趋势方向={}",
                    lastWindow.getStartDate(), lastWindow.getEndDate(),
                    BigDecimal.valueOf(lastWindowTotalSales).setScale(SCALE, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(salesForecast).setScale(SCALE, RoundingMode.HALF_UP),
                    forecastTrendDirection);
            }

            // 计算预测准确性指标
            BigDecimal forecastAccuracy = calculateForecastAccuracy(windowDataList, salesForecast, dailySalesData);
            String accuracyLevel = getForecastAccuracyLevel(forecastAccuracy);

            logger.debug("预测准确性评估: 准确性指数={}, 等级={}", forecastAccuracy, accuracyLevel);

            // 生成预测结果
            ForecastResult result = ForecastResult.builder()
                .methodResults(methodResults)
                .finalSalesForecast(BigDecimal.valueOf(salesForecast).setScale(SCALE, RoundingMode.HALF_UP))
                .finalProfitForecast(profitForecast)
                .lowerBound(BigDecimal.valueOf(lowerBound).setScale(SCALE, RoundingMode.HALF_UP))
                .upperBound(BigDecimal.valueOf(upperBound).setScale(SCALE, RoundingMode.HALF_UP))
                .productForecasts(productForecasts)
                .historicalTrendDirection(historicalTrendDirection)
                .forecastTrendDirection(forecastTrendDirection)
                .trendReliability(trendReliability)
                .forecastAccuracy(forecastAccuracy)
                .accuracyLevel(accuracyLevel)
                .build();

            logger.info("预测完成。销售预测: {}, 利润预测: {}, 准确性: {}",
                result.getFinalSalesForecast(), result.getFinalProfitForecast(),
                result.getAccuracyLevel());

            return result;
        } catch (Exception e) {
            logger.error("生成预测过程中发生错误: {}", e.getMessage(), e);
            return createEmptyForecast();
        }
    }

    /**
     * 获取当前在售商品ID列表
     * 注意：此方法需要根据您实际的ProductService接口进行调整
     */
    private Set<Integer> getActiveProductIds() {

            List<Product> productList = productService.findList(ProductQuery.builder()
                                                                            .isDel(false)
                                                                            .build());
        return  productList.stream()
                                               .map(Product::getId)
                                               .collect(Collectors.toSet());

    }

    /**
     * 创建固定大小的滑动窗口数据
     * 从结束日期反向创建窗口，确保最新数据被完整包含
     */
    private List<WindowSalesData> createSlidingWindows(List<DailySales> dailySalesData, int windowSize) {
        List<WindowSalesData> windows = new ArrayList<>();

        if (dailySalesData == null || dailySalesData.isEmpty()) {
            logger.warn("销售数据为空，无法创建窗口");
            return windows;
        }

        // 确保数据按日期排序
        dailySalesData.sort(Comparator.comparing(DailySales::getDate));

        if (dailySalesData.size() < windowSize) {
            // 如果数据不足一个窗口，创建一个包含所有数据的窗口
            WindowSalesData window = new WindowSalesData();
            LocalDate startDate = dailySalesData.get(0).getDate();
            LocalDate endDate = dailySalesData.get(dailySalesData.size() - 1).getDate();
            window.setStartDate(startDate);
            window.setEndDate(endDate);

            for (DailySales sale : dailySalesData) {
                window.addDailySales(sale.getSales(), sale.getProfit());
            }
            windows.add(window);
            logger.debug("数据不足一个完整窗口，创建单一窗口: 起始日期={}, 结束日期={}, 包含{}天数据",
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

            for (int i = windowStartIndex; i <= windowEndIndex; i++) {
                window.addDailySales(dailySalesData.get(i).getSales(), dailySalesData.get(i).getProfit());
            }

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

            for (int i = 0; i < remainingDays; i++) {
                partialWindow.addDailySales(dailySalesData.get(i).getSales(), dailySalesData.get(i).getProfit());
            }

            tempWindows.add(partialWindow);
        }

        // 将窗口按时间顺序重新排列（从早到晚）
        tempWindows.sort(Comparator.comparing(WindowSalesData::getStartDate));

        // 重新编号窗口并添加到结果列表
        for (int i = 0; i < tempWindows.size(); i++) {
            WindowSalesData window = tempWindows.get(i);
            windows.add(window);
        }

        logger.debug("成功创建{}个滑动窗口，第一个窗口: {}至{}, 最后一个窗口: {}至{}",
            windows.size(),
            windows.get(0).getStartDate(), windows.get(0).getEndDate(),
            windows.get(windows.size()-1).getStartDate(), windows.get(windows.size()-1).getEndDate());

        return windows;
    }

    /**
     * 创建空的预测结果
     */
    private ForecastResult createEmptyForecast() {
        return ForecastResult.builder()
                            .methodResults(new HashMap<>())
                            .finalSalesForecast(BigDecimal.ZERO)
                            .finalProfitForecast(BigDecimal.ZERO)
                            .lowerBound(BigDecimal.ZERO)
                            .upperBound(BigDecimal.ZERO)
                            .productForecasts(new HashMap<>())
                            .historicalTrendDirection("无")
                            .forecastTrendDirection("无")
                            .trendReliability(BigDecimal.ZERO)
                            .forecastAccuracy(BigDecimal.valueOf(0))
                            .accuracyLevel("无数据")
                            .build();
    }

    /**
     * 计算简单指数平滑预测
     */
    private double calculateSimpleExponentialSmoothing(double[] data, double alpha) {
        if (data.length == 0) {
            return 0;
        }

        double smoothed = data[0];

        for (int i = 1; i < data.length; i++) {
            smoothed = alpha * data[i] + (1 - alpha) * smoothed;
        }

        // 简单预测：按照最新平滑值
        return smoothed;
    }

    /**
     * 计算考虑季节性的预测
     */
    private double calculateSeasonalAdjustedForecast(double[] data) {
        if (data.length < 4) {
            // 不足以计算季节性，回退到简单预测
            return calculateSimpleExponentialSmoothing(data, 0.3);
        }

        // 1. 计算季节性指数（假设4个窗口为一个周期）
        double[] seasonalIndices = calculateSeasonalIndices(data);

        // 2. 计算去季节性的数据
        double[] deseasonalizedData = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            int season = i % 4;
            deseasonalizedData[i] = data[i] / seasonalIndices[season];
        }

        // 3. 对去季节性数据应用指数平滑
        double smoothed = calculateSimpleExponentialSmoothing(deseasonalizedData, 0.3);

        // 4. 重新应用季节性
        int nextSeason = data.length % 4;
        return smoothed * seasonalIndices[nextSeason];
    }

    /**
     * 计算季节性指数
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
     * 计算简单变异系数，用于预测区间
     */
    private double calculateSimpleVariability(List<DailySales> dailySales) {
        if (dailySales.size() < 2) {
            return 0.3; // 默认变异系数
        }

        // 计算近期变异系数
        List<Double> recentSales = dailySales.stream()
                                            .skip(Math.max(0, dailySales.size() - 30)) // 最近30天
                                                .map(DailySales::getSales)
                                                .collect(Collectors.toList());

        double mean = recentSales.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        if (mean < 1e-10) return 0.3;

        double variance = recentSales.stream()
                                    .mapToDouble(s -> Math.pow(s - mean, 2))
                                    .average()
                                    .orElse(0);

        double cv = Math.sqrt(variance) / mean;

        // 限制在合理范围内
        return Math.max(0.2, Math.min(0.5, cv));
    }

    /**
     * 计算窗口趋势可靠性指标
     */
    private BigDecimal calculateWindowTrendReliability(List<WindowSalesData> windowDataList) {
        if (windowDataList.size() < 3) {
            return BigDecimal.valueOf(0);
        }

        // 计算趋势一致性
        int consistentTrends = 0;
        for (int i = 2; i < windowDataList.size(); i++) {
            boolean prevTrend = windowDataList.get(i-1).getTotalSales() > windowDataList.get(i-2).getTotalSales();
            boolean currTrend = windowDataList.get(i).getTotalSales() > windowDataList.get(i-1).getTotalSales();
            if (prevTrend == currTrend) {
                consistentTrends++;
            }
        }

        double consistency = (double)consistentTrends / (windowDataList.size() - 2);

        // 计算噪声水平（使用变异系数）
        double[] salesData = windowDataList.stream()
                .mapToDouble(WindowSalesData::getTotalSales)
                .toArray();

        double mean = Arrays.stream(salesData).average().orElse(0);
        if (mean < 1e-10) return BigDecimal.valueOf(0);

        double variance = Arrays.stream(salesData)
                               .map(d -> Math.pow(d - mean, 2))
                               .average()
                               .orElse(0);

        double cv = Math.sqrt(variance) / mean;

        // 计算趋势可靠性指标
        double reliability = consistency * (1 - Math.min(1, cv));

        return BigDecimal.valueOf(reliability).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算平均利润率
     */
    private BigDecimal calculateAverageProfitRate(List<DailySales> dailySales) {
        if (dailySales.isEmpty()) {
            return BigDecimal.valueOf(0.25); // 默认25%利润率
        }

        // 只考虑有销售的日期
        List<DailySales> validSales = dailySales.stream()
                                              .filter(sale -> sale.getSales() > 0)
                                              .collect(Collectors.toList());

        if (validSales.isEmpty()) {
            return BigDecimal.valueOf(0.25);
        }

        // 计算平均利润率
        double totalSales = validSales.stream().mapToDouble(DailySales::getSales).sum();
        double totalProfit = validSales.stream().mapToDouble(DailySales::getProfit).sum();

        return totalSales > 0 ?
                BigDecimal.valueOf(totalProfit / totalSales).setScale(4, RoundingMode.HALF_UP) :
                BigDecimal.valueOf(0.25);
    }

    /**
     * 基于滑动窗口的产品级预测生成，只预测在售商品
     */
    private Map<String, ProductForecast> generateProductForecastsFromWindows(
            List<ProductSales> productSales,
            int windowSize,
            Set<Integer> activeProductIds) {

        if (productSales == null || productSales.isEmpty()) {
            logger.warn("没有产品销售数据可用于预测");
            return Collections.emptyMap();
        }

        logger.debug("开始生成产品级预测，基于{}条产品销售记录", productSales.size());

        // 获取全局数据的时间范围
        LocalDate globalStartDate = productSales.stream()
                .min(Comparator.comparing(ProductSales::getDate))
                .map(ProductSales::getDate)
                .orElse(null);

        LocalDate globalEndDate = productSales.stream()
                .max(Comparator.comparing(ProductSales::getDate))
                .map(ProductSales::getDate)
                .orElse(null);

        if (globalStartDate == null || globalEndDate == null) {
            logger.warn("无法确定销售数据的全局时间范围");
            return Collections.emptyMap();
        }

        // 按产品ID分组
        Map<Integer, List<ProductSales>> salesByProduct = productSales.stream()
                .collect(Collectors.groupingBy(ProductSales::getProductId));

        Map<String, ProductForecast> forecasts = new HashMap<>();
        int processedProductCount = 0;
        int activeProductCount = 0;

        // 只处理在售产品
        for (Map.Entry<Integer, List<ProductSales>> entry : salesByProduct.entrySet()) {
            Integer productId = entry.getKey();
            processedProductCount++;

            // 过滤非在售商品
            if (!activeProductIds.isEmpty() && !activeProductIds.contains(productId)) {
                continue;
            }
            activeProductCount++;

            List<ProductSales> productHistory = entry.getValue();

            // 创建产品销售窗口数据，使用全局时间范围
            List<ProductWindowData> productWindows = createProductWindows(productHistory, windowSize, globalStartDate, globalEndDate);

            if (productWindows.isEmpty()) {
                continue;
            }

            // 获取产品信息
            String productName = productHistory.get(0).getProductName();

            // 获取最近一个窗口的销量
            ProductWindowData lastWindow = productWindows.get(productWindows.size() - 1);
            int lastWindowSales = lastWindow.getTotalQuantity();

            // 确定产品类型和增长率
            ProductCategory category = categorizeProduct(productWindows);
            double growthRate = determineGrowthRate(productWindows, category);

            // 计算预测销量
            int predictedSales = Math.max(1, (int)Math.round(lastWindowSales * (1 + growthRate)));

            // 如果商品在最后一个窗口没有销售记录，特别处理
            if (lastWindowSales == 0) {
                logger.debug("产品「{}」在最后窗口没有销售记录，查找前一窗口销量", productName);

                // 找到最后一个有销量的窗口
                for (int i = productWindows.size() - 2; i >= 0; i--) {
                    ProductWindowData prevWindow = productWindows.get(i);
                    if (prevWindow.getTotalQuantity() > 0) {
                        lastWindowSales = prevWindow.getTotalQuantity();
                        predictedSales = Math.max(1, (int)Math.round(lastWindowSales * (1 + growthRate)));
                        break;
                    }
                }

                // 如果所有窗口都没有销量，设置为1
                if (lastWindowSales == 0) {
                    lastWindowSales = 1;
                    predictedSales = 1;
                }
            }

            // 计算平均单价和利润率
            double avgPrice = productHistory.stream()
                    .filter(s -> s.getQuantity() > 0)
                    .mapToDouble(s -> s.getSales() / s.getQuantity())
                    .filter(price -> !Double.isNaN(price) && !Double.isInfinite(price))
                    .average()
                    .orElse(0);

            double avgProfitRate = productHistory.stream()
                    .filter(s -> s.getSales() > 0)
                    .mapToDouble(s -> s.getProfit() / s.getSales())
                    .filter(rate -> !Double.isNaN(rate) && !Double.isInfinite(rate))
                    .average()
                    .orElse(0.25);

            // 计算预测销售额和利润
            BigDecimal salesAmount = BigDecimal.valueOf(predictedSales * avgPrice)
                                               .setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal profitAmount = salesAmount.multiply(BigDecimal.valueOf(avgProfitRate))
                                                .setScale(SCALE, RoundingMode.HALF_UP);

            // 创建产品预测
            ProductForecast forecast = ProductForecast.builder()
                .productId(productId)
                .productName(productName)
                .lastMonthSales(lastWindowSales)
                .predictedSales(predictedSales)
                .growthRate(BigDecimal.valueOf(growthRate).setScale(4, RoundingMode.HALF_UP))
                .salesAmount(salesAmount)
                .profitAmount(profitAmount)
                .build();

            forecasts.put(productName, forecast);
        }

        logger.debug("产品级预测完成：处理{}个产品，生成{}个在售产品的预测",
                processedProductCount, activeProductCount);

        return forecasts;
    }

    /**
     * 创建产品销售窗口数据
     * 从结束日期反向创建窗口，确保最新数据被完整包含
     * 使用全局开始和结束日期，确保所有产品使用相同的窗口结构
     */
    private List<ProductWindowData> createProductWindows(
            List<ProductSales> productSales,
            int windowSize,
            LocalDate globalStartDate,
            LocalDate globalEndDate) {

        // 确保销售数据按日期排序
        productSales.sort(Comparator.comparing(ProductSales::getDate));

        // 创建窗口
        List<ProductWindowData> windows = new ArrayList<>();

        if (productSales.isEmpty()) {
            return windows;
        }

        String productName = productSales.get(0).getProductName();

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
            logger.trace("产品「{}」创建了{}个窗口", productName, windows.size());
        }

        return windows;
    }

    /**
     * 产品类别简化为三类
     */
    private enum ProductCategory {
        HIGH_VOLUME,
        MEDIUM_VOLUME,
        LOW_VOLUME
    }

    /**
     * 根据销量历史对产品进行分类
     */
    private ProductCategory categorizeProduct(List<ProductWindowData> productWindows) {
        if (productWindows.isEmpty()) {
            return ProductCategory.LOW_VOLUME;
        }

        // 计算平均窗口销量
        double avgQuantity = productWindows.stream()
                .mapToInt(ProductWindowData::getTotalQuantity)
                .average()
                .orElse(0);

        if (avgQuantity >= 50) {
            return ProductCategory.HIGH_VOLUME;
        } else if (avgQuantity >= 10) {
            return ProductCategory.MEDIUM_VOLUME;
        } else {
            return ProductCategory.LOW_VOLUME;
        }
    }

    /**
     * 根据产品类别和窗口数据确定增长率
     */
    private double determineGrowthRate(List<ProductWindowData> productWindows, ProductCategory category) {
        if (productWindows.size() < 2) {
            return 0.0;
        }

        // 计算窗口间增长率
        double totalGrowth = 0;
        int countPeriods = 0;

        for (int i = 1; i < productWindows.size(); i++) {
            ProductWindowData prevWindow = productWindows.get(i-1);
            ProductWindowData currWindow = productWindows.get(i);

            int prevQuantity = prevWindow.getTotalQuantity();
            int currQuantity = currWindow.getTotalQuantity();

            if (prevQuantity > 0) {
                double periodGrowth = (double)(currQuantity - prevQuantity) / prevQuantity;
                totalGrowth += periodGrowth;
                countPeriods++;
            }
        }

        double avgGrowth = countPeriods > 0 ? totalGrowth / countPeriods : 0;

        // 根据产品类别调整增长率
        double adjustedGrowth;
        switch (category) {
            case HIGH_VOLUME:
                // 高销量产品增长较为稳定
                adjustedGrowth = Math.max(-0.2, Math.min(0.2, avgGrowth));
                break;

            case MEDIUM_VOLUME:
                // 中等销量产品增长适中
                adjustedGrowth = Math.max(-0.25, Math.min(0.25, avgGrowth));
                break;

            case LOW_VOLUME:
                // 低销量产品增长波动较大，使用更保守的预测
                adjustedGrowth = Math.max(-0.3, Math.min(0.3, avgGrowth * 0.8));
                break;

            default:
                adjustedGrowth = 0;
        }

        return adjustedGrowth;
    }

    // =================== 数据模型 ===================

    /**
     * 每日销售数据
     */
    private static class DailySales {
        private final LocalDate date;
        private final double sales;
        private final double profit;

        public DailySales(LocalDate date, double sales, double profit) {
            this.date = date;
            this.sales = sales;
            this.profit = profit;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getSales() {
            return sales;
        }

        public double getProfit() {
            return profit;
        }
    }

    /**
     * 产品销售数据
     */
    private static class ProductSales {
        private final LocalDate date;
        private final int productId;
        private final String productName;
        private final int quantity;
        private final double sales;
        private final double profit;

        public ProductSales(LocalDate date, int productId, String productName, int quantity, double sales, double profit) {
            this.date = date;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.sales = sales;
            this.profit = profit;
        }

        public LocalDate getDate() {
            return date;
        }

        public int getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getSales() {
            return sales;
        }

        public double getProfit() {
            return profit;
        }
    }

    /**
     * 窗口销售数据
     */
    private static class WindowSalesData {
        private double totalSales = 0;
        private double totalProfit = 0;
        private int dayCount = 0;
        private LocalDate startDate;  // 窗口起始日期
        private LocalDate endDate;    // 窗口结束日期

        public void addDailySales(double sales, double profit) {
            totalSales += sales;
            totalProfit += profit;
            dayCount++;
        }

        public double getTotalSales() {
            return totalSales;
        }

        public double getAvgDailySales() {
            return dayCount > 0 ? totalSales / dayCount : 0;
        }

        public double getAvgDailyProfit() {
            return dayCount > 0 ? totalProfit / dayCount : 0;
        }

        public int getDayCount() {
            return dayCount;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
    }

    /**
     * 产品窗口销售数据
     */
    private static class ProductWindowData {
        private int totalQuantity = 0;
        private double totalSales = 0;
        private double totalProfit = 0;
        private Set<LocalDate> dates = new HashSet<>();
        private LocalDate startDate;  // 窗口起始日期
        private LocalDate endDate;    // 窗口结束日期

        public void addSales(ProductSales sale) {
            totalQuantity += sale.getQuantity();
            totalSales += sale.getSales();
            totalProfit += sale.getProfit();
                dates.add(sale.getDate());
        }

        public int getTotalQuantity() {
            return totalQuantity;
        }

        public double getTotalSales() {
            return totalSales;
        }

        public double getTotalProfit() {
            return totalProfit;
        }

        public int getDayCount() {
            return dates.size();
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
    }

    /**
     * 预测结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastResult {
        private Map<String, BigDecimal> methodResults;     // 各种预测方法的结果
        private BigDecimal finalSalesForecast;             // 最终销售额预测
        private BigDecimal finalProfitForecast;            // 最终利润预测
        private BigDecimal lowerBound;                     // 预测下限
        private BigDecimal upperBound;                     // 预测上限
        private Map<String, ProductForecast> productForecasts; // 产品级预测
        private String historicalTrendDirection;           // 历史趋势方向（上升/下降）
        private String forecastTrendDirection;             // 预测趋势方向（上升/下降）
        private BigDecimal trendReliability;               // 趋势可靠性指标
        private BigDecimal forecastAccuracy;               // 预测值准确性指标
        private String accuracyLevel;                      // 预测准确性等级描述
    }

    /**
     * 产品预测
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductForecast {
        private int productId;               // 产品ID
        private String productName;          // 产品名称
        private int lastMonthSales;          // 上个窗口销量
        private int predictedSales;          // 预测销量
        private BigDecimal growthRate;       // 增长率
        private BigDecimal salesAmount;      // 预测销售额
        private BigDecimal profitAmount;     // 预测利润
    }

    /**
     * 计算预测准确性指标
     * 基于数据稳定性、数据充足度、季节性强度和最近波动性等因素
     * 返回0-1之间的值，越高表示预测越可能准确
     */
    private BigDecimal calculateForecastAccuracy(List<WindowSalesData> windowDataList, double salesForecast, List<DailySales> dailySalesData) {
        if (windowDataList == null || windowDataList.size() < 2) {
            return BigDecimal.valueOf(0.3); // 数据不足，准确度较低
        }

        try {
            // 1. 数据稳定性评估 (使用变异系数)
            double[] salesData = windowDataList.stream()
                    .mapToDouble(WindowSalesData::getTotalSales)
                    .toArray();

            double mean = Arrays.stream(salesData).average().orElse(0);
            if (mean < 1e-10) return BigDecimal.valueOf(0.3);

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

                double lastSales = lastWindow.getTotalSales();
                double prevSales = prevWindow.getTotalSales();

                if (prevSales > 0) {
                    recentVolatility = Math.min(1.0, Math.abs((lastSales - prevSales) / prevSales));
                }
            }

            // 5. 历史预测与实际值的偏差
            double dailyDataStability = 0.0;
            if (dailySalesData != null && dailySalesData.size() >= FORECAST_DAYS * 2) {
                List<Double> recentSales = dailySalesData.stream()
                                                      .skip(Math.max(0, dailySalesData.size() - FORECAST_DAYS * 2))
                                                      .map(DailySales::getSales)
                                                      .collect(Collectors.toList());

                // 计算相邻时间段的平均偏差
                double totalDeviation = 0.0;
                int deviationCount = 0;

                for (int i = 0; i < recentSales.size() - FORECAST_DAYS; i++) {
                    double period1Avg = 0.0;
                    for (int j = 0; j < FORECAST_DAYS; j++) {
                        period1Avg += recentSales.get(i + j);
                    }
                    period1Avg /= FORECAST_DAYS;

                    double period2Avg = 0.0;
                    for (int j = 0; j < FORECAST_DAYS; j++) {
                        if (i + FORECAST_DAYS + j < recentSales.size()) {
                            period2Avg += recentSales.get(i + FORECAST_DAYS + j);
                        }
                    }
                    period2Avg /= FORECAST_DAYS;

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

            logger.trace("预测准确性评估详情: 数据稳定性={}, 数据充足度={}, 季节性强度={}, 最近波动性={}, 日数据稳定性={}",
                        BigDecimal.valueOf(stability).setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(dataSufficiency).setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(seasonality).setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(recentVolatility).setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(dailyDataStability).setScale(4, RoundingMode.HALF_UP));

            return BigDecimal.valueOf(accuracy).setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            logger.error("计算预测准确性时发生错误: {}", e.getMessage(), e);
            return BigDecimal.valueOf(0.3); // 发生错误时返回默认值
        }
    }

    /**
     * 获取预测准确性等级描述
     */
    private String getForecastAccuracyLevel(BigDecimal forecastAccuracy) {
        double value = forecastAccuracy.doubleValue();

        if (value >= 0.8) {
            return "高度准确";
        } else if (value >= 0.6) {
            return "较为准确";
        } else if (value >= 0.4) {
            return "中等准确";
        } else {
            return "参考性有限";
        }
    }
}