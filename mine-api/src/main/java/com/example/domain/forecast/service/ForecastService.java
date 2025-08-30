package com.example.domain.forecast.service;

import com.example.domain.forecast.strategy.ForecastStrategy;
import com.example.domain.product.service.ProductService;
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
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
    
    // 预测任务的线程池
    private Executor forecastExecutor;
    
    @Value("${forecast.threadpool.size:10}")
    private int threadPoolSize;
    
    @Value("${prophet.max-connections:20}")
    private int prophetMaxConnections;

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
     * 初始化线程池和HTTP连接池
     */
    @PostConstruct
    public void init() {
        // 创建预测任务专用的线程池
        forecastExecutor = Executors.newFixedThreadPool(threadPoolSize);
        log.info("初始化预测服务线程池，大小: {}", threadPoolSize);
        
        // 配置Prophet服务的HTTP连接池
        configProphetHttpClient();
        
        log.info("预测服务初始化完成");
    }
    
    /**
     * 配置与Prophet服务通信的HTTP客户端
     * 这里只是一个示例，实际实现需要根据具体使用的HTTP客户端库
     */
    private void configProphetHttpClient() {
        // 这里应该实现对HTTP客户端的配置
        // 例如，如果使用OkHttp或Apache HttpClient，应该在这里配置连接池
        log.info("配置Prophet服务HTTP连接池，最大连接数: {}", prophetMaxConnections);
        
        // 具体实现示例(使用Apache HttpClient的情况):
        // PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        // connManager.setMaxTotal(prophetMaxConnections);
        // connManager.setDefaultMaxPerRoute(prophetMaxConnections);
        // httpClient = HttpClients.custom().setConnectionManager(connManager).build();
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
        log.debug("商品ID: {}, 原始日数据长度: {}", productId, rawDailySales.length);

        // 周聚合数据不再在此处统一计算，策略如果需要会自行聚合
        // double[] weeklyData = aggregateToWeeklyData(rawDailySales);

        ForecastStrategy selectedStrategy = strategies.stream()
                                                    // 现在 canHandle 接收原始日数据长度
                                                    .filter(strategy -> strategy.canHandle(rawDailySales.length))
                                                    // 按 getMinDataLength 降序排序，优先选择数据需求量大的策略
                                                    .sorted(Comparator.comparingInt(ForecastStrategy::getMinDataLength).reversed())
                                                    .findFirst()
                                                    .orElseThrow(() -> new MyException("没有可用的预测策略"));

        // 4. 执行预测
        double[] dailyForecast;
        try {
            // 所有策略的 forecast 方法现在都接收原始日数据
            log.debug("商品ID: {}, 选择策略: {}, 传递原始日数据 (长度:{}) 进行预测。", productId, selectedStrategy.getStrategyName(), rawDailySales.length);
            dailyForecast = selectedStrategy.forecast(rawDailySales, forecastDays);
        } catch (Exception e) {
            log.error("商品ID: {} 使用策略 {} 预测失败: {}", productId, selectedStrategy.getStrategyName(), e.getMessage(), e);

            return new double[]{0.0};
        }

        return dailyForecast;
    }
    
    /**
     * 异步预测单个商品的未来销量总和。
     * 与同步版本功能相同，但返回CompletableFuture以支持异步调用。
     *
     * @param productId         商品ID。
     * @param historicalDataMap 包含按日期组织的销售统计数据的 Map。
     * @param forecastDays      需要预测的未来天数。
     * @return 包含预测结果的CompletableFuture。如果无法预测，则返回包含{0.0}的CompletableFuture。
     */
    @Async
    public CompletableFuture<double[]> forecastProductTotalAsync(int productId, Map<LocalDate, SalesStatisticsDTO> historicalDataMap, int forecastDays) {
        try {
            log.debug("异步开始预测商品ID: {}", productId);
            double[] result = forecastProductTotal(productId, historicalDataMap, forecastDays);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("商品ID: {} 异步预测失败: {}", productId, e.getMessage(), e);
            return CompletableFuture.completedFuture(new double[]{0.0});
        }
    }
    
    /**
     * 使用自定义线程池进行异步预测。
     * 比@Async注解更灵活，可以指定使用专用的线程池。
     *
     * @param productId         商品ID。
     * @param historicalDataMap 包含按日期组织的销售统计数据的 Map。
     * @param forecastDays      需要预测的未来天数。
     * @return 包含预测结果的CompletableFuture。
     */
    public CompletableFuture<double[]> forecastProductTotalWithCustomExecutor(int productId, Map<LocalDate, SalesStatisticsDTO> historicalDataMap, int forecastDays) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("使用自定义线程池开始预测商品ID: {}", productId);
                return forecastProductTotal(productId, historicalDataMap, forecastDays);
            } catch (Exception e) {
                log.error("商品ID: {} 预测失败: {}", productId, e.getMessage(), e);
                return new double[]{0.0};
            }
        }, forecastExecutor);
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
    public static double[] aggregateToWeeklyData(double[] data) { // 改为 public static
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
