package com.example.domain.forecast.service;

import com.example.domain.forecast.strategy.ForecastStrategy;
import com.example.domain.product.service.ProductService; // 确保已导入
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO; // 确保已导入
import com.example.domain.statistics.dto.response.SalesStatisticsDTO; // 确保已导入
import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预测服务类。
 * 负责协调预测流程，包括数据准备、策略选择和执行预测。
 */
@Service
@Slf4j
public class ForecastService {

    private final List<ForecastStrategy> strategies;
    private final ProductService productService; // 注入 ProductService

    /**
     * 构造函数，注入所有可用的预测策略和 ProductService。
     * 策略会根据其适用的最小数据长度进行排序，优先选择能处理更多数据的策略。
     *
     * @param strategies     Spring 自动注入的所有 ForecastStrategy Bean 列表。
     * @param productService ProductService 用于获取商品相关信息。
     */
    @Autowired
    public ForecastService(List<ForecastStrategy> strategies, ProductService productService) {
        // 按 getMinDataLength 降序排序，优先匹配需要更多数据的策略
        this.strategies = strategies.stream()
                                    .sorted(Comparator.comparingInt(ForecastStrategy::getMinDataLength)
                                                      .reversed())
                                    .toList();
        this.productService = productService; // 保存注入的 ProductService
        log.info("加载并排序的预测策略: {}", this.strategies.stream()
                                                            .map(ForecastStrategy::getStrategyName)
                                                            .collect(Collectors.joining(", ")));
    }

    /**
     * 预测单个商品的未来销量总和。
     * 这是供外部（如 PurchaseService）调用的主要预测方法。
     *
     * @param productId         商品ID。
     * @param historicalDataMap 包含按日期组织的销售统计数据的 Map。
     * @param forecastDays      需要预测的未来天数。
     * @return 预测的未来 N 天总销量。如果无法预测（如无历史数据），则返回 0。
     * @throws MyException 如果在预测过程中发生不可恢复的错误。
     */
    public double forecastProductTotal(int productId, Map<LocalDate, SalesStatisticsDTO> historicalDataMap, int forecastDays) throws MyException {
        // 1. 提取该商品的原始日销量数据
        double[] rawDailySales = getProductData(historicalDataMap, productId);
        log.debug("商品ID: {}, 原始数据长度: {}", productId, rawDailySales.length);

        if (rawDailySales.length == 0) {
            log.warn("商品ID: {} 历史数据为空，无法预测。", productId);
            return 0.0; // 对于无历史数据的商品，预测销量为0
        }

        // 2. 数据预处理 (例如：7日移动平均平滑) - 可选步骤
        // 如果需要，在此处调用 applyDataPreprocessing
        double[] processedData = applyDataPreprocessing(rawDailySales);
        log.debug("商品ID: {}, 处理后数据长度: {}", productId, processedData.length);

        // 3. 选择合适的预测策略 (已排序，取第一个满足条件的)
        ForecastStrategy selectedStrategy = strategies.stream()
                                                      .filter(strategy -> strategy.canHandle(processedData.length))
                                                      .findFirst()
                                                      // 使用 orElse(null) 或 Optional 处理找不到策略的情况
                                                      .orElse(null);

        // 如果没有找到合适的策略（例如数据量 < 30），则不进行预测
        if (selectedStrategy == null) {
            log.warn("商品ID: {} 数据量 ({}) 过少，没有适用的预测策略。", productId, processedData.length);
            throw new MyException("商品ID: " + productId + " 没有找到适合数据量 " + processedData.length + " 的预测策略");
        }

        log.info("商品ID: {}, 数据长度: {}, 选择策略: {}", productId, processedData.length, selectedStrategy.getStrategyName());

        // 4. 执行预测
        double[] dailyForecast;
        try {
            dailyForecast = selectedStrategy.forecast(processedData, forecastDays);
        } catch (Exception e) {
            log.error("商品ID: {} 使用策略 {} 预测失败: {}", productId, selectedStrategy.getStrategyName(), e.getMessage(), e);
            // 预测失败时，可以选择返回 0 或重新抛出异常，这里选择返回 0 并记录错误
            // throw new MyException("商品ID: " + productId + " 预测失败", e);
            log.warn("商品ID: {} 预测失败，将返回预测值 0。", productId);
            return 0.0;
        }

        // 5. 计算并返回总预测量
        double totalForecast = Arrays.stream(dailyForecast)
                                     .sum();
        log.debug("商品ID: {}, 预测未来 {} 天总销量: {}", productId, forecastDays, totalForecast);

        // 确保预测结果非负
        return Math.max(0, totalForecast);
    }

    /**
     * 数据预处理：应用7日移动平均 (示例)。
     * 可以根据需要启用或修改此方法。
     *
     * @param rawData 原始日销量数据。
     * @return 处理后的数据。
     */
    private double[] applyDataPreprocessing(double[] rawData) {
        // 当前设计中，预处理逻辑（如移动平均）已包含在部分策略或 HoltWintersForecast 工具类中
        // 此处可以选择是否进行额外的、通用的预处理
        // 例如，如果所有策略都需要7日移动平均，可以在这里统一处理：
        // int movingAverageWindow = 7;
        // if (rawData.length >= movingAverageWindow) {
        //     log.debug("应用 {} 日移动平均进行预处理...", movingAverageWindow);
        //     return HoltWintersForecast.applySimpleMovingAverage(rawData, movingAverageWindow);
        // } else {
        //     log.debug("数据量不足，跳过移动平均预处理。");
        //     return rawData;
        // }
        // 暂时不进行通用预处理，让策略自行决定或使用原始数据
        log.debug("数据预处理步骤：暂未应用通用平滑处理。");
        return rawData;
    }

    /**
     * 从按日期组织的销售统计数据中提取指定商品的日销量数组。
     *
     * @param data      包含历史销售统计的 Map (LocalDate -> SalesStatisticsDTO)。
     * @param productId 需要提取数据的商品 ID。
     * @return 按时间顺序排列的该商品日销量数组。如果找不到商品或无销售记录，则返回空数组。
     */
    public double[] getProductData(Map<LocalDate, SalesStatisticsDTO> data, int productId) {
        if (data == null || data.isEmpty()) {
            return new double[0];
        }
        // 查找该商品的最早销售日期，以确定数据提取的起始点
        LocalDate earliestSaleDate = productService.getEarliestSaleDateByProductId(productId);
        if (earliestSaleDate == null) {
            log.warn("无法获取商品ID: {} 的最早销售日期，可能无销售记录。", productId);
            return new double[0];
        }

        // 过滤掉早于最早销售日期的数据，并按日期排序
        List<Map.Entry<LocalDate, SalesStatisticsDTO>> sortedEntries = data.entrySet()
                                                                           .stream()
                                                                           .filter(entry -> !entry.getKey()
                                                                                                  .isBefore(earliestSaleDate))
                                                                           .sorted(Map.Entry.comparingByKey())
                                                                           .toList();

        // 从每日统计中提取该商品的销量
        double[] dailySales = sortedEntries.stream()
                                           .mapToDouble(entry ->
                                                   entry.getValue()
                                                        .getProductSalesInfoDTOS() // 获取当天所有商品的销售信息
                                                        .stream()
                                                        .filter(dto -> dto.getProductId() == productId) // 找到目标商品
                                                        .mapToInt(ProductSalesInfoDTO::getQuantity) // 获取销量
                                                        .findFirst() // 应该只有一个匹配项
                                                        .orElse(0) // 如果当天该商品未售出，销量为 0
                                           )
                                           .toArray();
        return dailySales;
    }

    // --- 移除旧的 forecast 和 aggregateToWeeklyData 方法 ---
    /*
    public double forecast(double[] data, int forecastDays) throws MyException {
        // ... 旧的实现 ...
    }

    public double[] aggregateToWeeklyData(double[] data) {
        // ... 旧的实现 ...
    }
    */

    // --- 保留可能仍然需要的其他公共方法，例如 getOverallSalesData ---

    /**
     * 提取总体销售额数据数组 (示例，如果其他地方需要)。
     *
     * @param data 历史统计数据 Map。
     * @return 总体销售额数组。
     */
    public double[] getOverallSalesData(Map<LocalDate, SalesStatisticsDTO> data) {
        if (data == null) return new double[0];
        return data.entrySet()
                   .stream()
                   .sorted(Map.Entry.comparingByKey())
                   .mapToDouble(entry -> entry.getValue()
                                              .getTotalSales()
                                              .doubleValue())
                   .toArray();
    }
}
