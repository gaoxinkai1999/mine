package com.example.domain.forecast.service;

import com.example.domain.forecast.strategy.ForecastStrategy;
import com.example.domain.product.service.ProductService;
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    public double[] forecastProductTotal(int productId, Map<LocalDate, SalesStatisticsDTO> historicalDataMap, int forecastDays) throws MyException {
        // 1. 提取该商品的原始日销量数据
        double[] rawDailySales = getProductData(historicalDataMap, productId);
        log.debug("商品ID: {}, 原始数据长度: {}", productId, rawDailySales.length);

        double[] weeklyData = aggregateToWeeklyData(rawDailySales);

        ForecastStrategy selectedStrategy = strategies.stream()
                                                    .filter(strategy -> strategy.canHandle(weeklyData.length))
                                                    .findFirst()
                                                    .orElseThrow(() -> new MyException("没有可用的预测策略"));

        // 4. 执行预测
        double[] dailyForecast;
        try {
            dailyForecast = selectedStrategy.forecast(weeklyData, forecastDays);
        } catch (Exception e) {
            log.error("商品ID: {} 使用策略 {} 预测失败: {}", productId, selectedStrategy.getStrategyName(), e.getMessage(), e);

            return new double[]{0.0};
        }

        return dailyForecast;
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

    /**
     * 将日销售数据聚合为周销售数据。
     * 如果天数不是7的整数倍，则从开头舍弃多余的天数。
     * 例如，10天数据会舍弃前3天，只聚合最后7天为1周。
     *
     * @param data 原始日销售数据
     * @return 按周聚合的销售数据
     */
    public double[] aggregateToWeeklyData(double[] data) {
        if (data == null || data.length < 7) {
            return new double[0];
        }
        int remainder = data.length % 7;
        int weeks = (data.length - remainder) / 7;
        double[] weeklyData = new double[weeks];
        for (int i = 0; i < weeks; i++) {
            double sum = 0;
            for (int j = 0; j < 7; j++) {
                sum += data[remainder + i * 7 + j];
            }
            weeklyData[i] = sum;
        }
        return weeklyData;
    }
}
