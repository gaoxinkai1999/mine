package com.example.domain.statistics.service;

import com.example.domain.statistics.dto.request.MovingAverageLineRequest;
import com.example.domain.statistics.dto.response.MovingAverageLineDTO;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.domain.statistics.dto.response.ShopStatisticsDTO;
import com.example.utils.DataExtractor; // Keep if still needed for getMovingAverage facade method
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function; // Keep if still needed for getMovingAverage facade method
import java.util.HashMap; // Keep if still needed for getMovingAverage facade method
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO; // Keep if still needed for getMovingAverage facade method


/**
 * 统计服务门面类
 * 协调调用不同的统计子服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticsService {

    private final ShopStatisticsService shopStatisticsService;
    private final TemporalStatisticsService temporalStatisticsService;
    private final MovingAverageStatisticsService movingAverageStatisticsService;

    // 移除 OrderService, ShopService, ProductService, JPAQueryFactory 的注入，
    // 它们现在由具体的子服务管理

    /**
     * 计算所有商家的统计数据 (委托给 ShopStatisticsService)
     *
     * @return List<ShopStatisticsDTO> 包含所有商家统计结果的列表
     */
    public List<ShopStatisticsDTO> calculateShopStatistics() {
        log.info("Calculating shop statistics...");
        return shopStatisticsService.calculateShopStatistics();
    }

    /**
     * 计算指定日期范围内每个月的销售统计数据 (委托给 TemporalStatisticsService)
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return Map<YearMonth, SalesStatisticsDTO> 按年月分组的统计数据
     */
    public Map<YearMonth, SalesStatisticsDTO> calculateMonthlyStatisticsOptimized(LocalDate startDate, LocalDate endDate) {
        log.info("Calculating monthly statistics from {} to {}", startDate, endDate);
        return temporalStatisticsService.calculateMonthlyStatisticsOptimized(startDate, endDate);
    }

    /**
     * 计算指定日期范围内的总体销售统计数据 (委托给 TemporalStatisticsService)
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return SalesStatisticsDTO 包含日期范围内总体统计和按商品分组的统计
     */
    public SalesStatisticsDTO calculateDateRangeStatistics(LocalDate startDate, LocalDate endDate) {
        log.info("Calculating date range statistics from {} to {}", startDate, endDate);
        return temporalStatisticsService.calculateDateRangeStatistics(startDate, endDate);
    }

    /**
     * 计算指定日期范围内每日的销售统计数据 (委托给 TemporalStatisticsService)
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return Map<LocalDate, SalesStatisticsDTO> 按日期分组的统计数据
     */
    public Map<LocalDate, SalesStatisticsDTO> calculateDailyStatistics(LocalDate startDate, LocalDate endDate) {
        log.info("Calculating daily statistics from {} to {}", startDate, endDate);
        return temporalStatisticsService.calculateDailyStatistics(startDate, endDate);
    }


    /**
     * 计算总体销售额和利润的移动平均趋势 (委托给 MovingAverageStatisticsService)
     *
     * @param period    移动平均周期（天数）
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 包含收入和利润移动平均数据的DTO对象
     */
    public MovingAverageLineDTO calculateOverallTrend(int period, LocalDate startDate, LocalDate endDate) {
        log.info("Calculating overall trend with period {} from {} to {}", period, startDate, endDate);
        return movingAverageStatisticsService.calculateOverallTrend(period, startDate, endDate);
    }


     // --- 处理产品移动平均的静态映射和facade方法 ---

     // 保留这个静态映射，因为它定义了业务逻辑转换
    private static final Map<MovingAverageLineRequest.TaskType, Function<ProductSalesInfoDTO, BigDecimal>> taskMap = new HashMap<>();

    static {
        // 任务映射
        taskMap.put(MovingAverageLineRequest.TaskType.Profit, ProductSalesInfoDTO::getTotalProfit);
        taskMap.put(MovingAverageLineRequest.TaskType.SalesAmount, ProductSalesInfoDTO::getTotalSales);
        taskMap.put(MovingAverageLineRequest.TaskType.Quantity, productSalesInfoDTO -> BigDecimal.valueOf(productSalesInfoDTO.getQuantity()));
    }

    /**
     * 获取指定类型的数据提取器
     * @param taskType 任务类型
     * @return DataExtractor
     */
     private DataExtractor getDataExtractorByType(MovingAverageLineRequest.TaskType taskType) {
         Function<ProductSalesInfoDTO, BigDecimal> function = taskMap.get(taskType);
         if (function == null) {
             throw new IllegalArgumentException("Unsupported task type: " + taskType);
         }
         // 将 Function<ProductSalesInfoDTO, BigDecimal> 适配为 DataExtractor 接口
         return function::apply;
     }


    /**
     * 计算多个产品的移动平均值 (委托给 MovingAverageStatisticsService)
     * 此方法作为 Facade，处理 TaskType 到 DataExtractor 的转换
     *
     * @param request   包含产品ID、任务类型、周期的请求对象
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 移动平均线DTO列表
     */
    public MovingAverageLineDTO getMovingAverage(MovingAverageLineRequest request, LocalDate startDate, LocalDate endDate) {
        log.info("Calculating moving average for products: {}, type: {}, period: {}, dates: {} - {}",
                 request.getProductIds(), request.getTaskType(), request.getPeriod(), startDate, endDate);

        // 1. 根据请求中的 TaskType 获取对应的 DataExtractor
        DataExtractor dataExtractor = getDataExtractorByType(request.getTaskType());

        // 2. 调用 MovingAverageStatisticsService 的方法
        return movingAverageStatisticsService.getMovingAverageForProducts(
                request.getProductIds(),
                dataExtractor,
                request.getPeriod(),
                startDate,
                endDate
        );
    }


    // 移除所有私有方法，它们已被移到相应的子服务中:
    // getShopAggregateStatistics
    // getShopProductMonthlyStatistics
    // createEmptySalesStatistics (在 TemporalStatisticsService 中)
    // calculateStatisticsForShop
    // getDailySalesForProduct (在 MovingAverageStatisticsService 中)

}
