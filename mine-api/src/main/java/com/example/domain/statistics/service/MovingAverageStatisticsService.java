package com.example.domain.statistics.service;

import com.example.domain.order.entity.Order;
import com.example.domain.order.service.OrderService;
import com.example.domain.product.entity.Product;
import com.example.domain.product.service.ProductService;
import com.example.domain.statistics.dto.request.MovingAverageLineRequest;
import com.example.domain.statistics.dto.response.MovingAverageLineDTO;
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.query.OrderQuery;
import com.example.query.ProductQuery;
import com.example.utils.DataExtractor;
import com.example.utils.MovingAverageCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 移动平均统计服务
 * 提供计算移动平均趋势的服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovingAverageStatisticsService {

    private final OrderService orderService; // 订单模块服务
    private final ProductService productService; // 商品模块服务
    private final TemporalStatisticsService temporalStatisticsService; // 依赖时间维度统计服务获取日数据

    /**
     * 计算总体销售额和利润的移动平均趋势
     *
     * @param period    移动平均周期（天数）
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 包含收入和利润移动平均数据的DTO对象
     */
    public MovingAverageLineDTO calculateOverallTrend(int period, LocalDate startDate, LocalDate endDate) {
        // 计算日期范围内的总天数
        long numOfDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // 生成连续的日期列表，确保日期的连续性
        List<LocalDate> localDates = Stream.iterate(startDate, date -> date.plusDays(1))
                                           .limit(numOfDays)
                                           .toList();

        // 初始化每日收入和利润的Map，使用LinkedHashMap保持日期顺序
        Map<LocalDate, BigDecimal> totalRevenueMap = localDates.stream()
                                                               .collect(Collectors.toMap(
                                                                       date -> date,
                                                                       date -> BigDecimal.ZERO,
                                                                       (v1, v2) -> v1,
                                                                       LinkedHashMap::new
                                                               ));
        Map<LocalDate, BigDecimal> totalProfitMap = new LinkedHashMap<>(totalRevenueMap);

        // 构建订单查询条件
        OrderQuery orderQuery = OrderQuery.builder()
                                          .startTime(startDate)
                                          .endTime(endDate)
                                          .build();

        // 查询指定日期范围内的订单数据
        List<Order> orders = orderService.findList(orderQuery);

        // 按日期汇总订单的收入和利润
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                LocalDate date = order.getCreateTime().toLocalDate();
                // 仅处理在日期范围内的日期，防止查询结果包含边界外数据（虽然理论上不应该）
                if (totalRevenueMap.containsKey(date)) {
                    totalRevenueMap.merge(date, order.getTotalSalesAmount(), BigDecimal::add);
                    totalProfitMap.merge(date, order.getTotalProfit(), BigDecimal::add);
                }
            }
        } else {
            // 没有订单记录，可以提前返回空数据或抛出异常，这里选择返回空数据
             log.warn("在日期范围 {} 到 {} 内没有找到订单记录，无法计算总体趋势移动平均。", startDate, endDate);
             // 返回一个包含日期但数据为空的 DTO
             MovingAverageLineDTO emptyDto = new MovingAverageLineDTO();
             emptyDto.setDates(localDates);
             emptyDto.setMovingAverageInfoDTOS(new ArrayList<>());
             return emptyDto;
             // 或者抛出异常
             // throw new RuntimeException("在日期范围 " + startDate + " 到 " + endDate + " 内没有找到订单记录");
        }


        // 将Map中的数据转换为double数组，保持日期顺序
        double[] revenueValues = localDates.stream()
                                           .map(totalRevenueMap::get)
                                           .mapToDouble(BigDecimal::doubleValue)
                                           .toArray();

        double[] profitValues = localDates.stream()
                                          .map(totalProfitMap::get)
                                          .mapToDouble(BigDecimal::doubleValue)
                                          .toArray();

        // 验证数据量是否足够计算移动平均
        if (revenueValues.length < period) {
             log.warn("数据点 ({}) 不足以计算周期为 {} 的移动平均值。", revenueValues.length, period);
             // 返回一个包含日期但数据为空的 DTO
             MovingAverageLineDTO insufficientDto = new MovingAverageLineDTO();
             insufficientDto.setDates(localDates);
             insufficientDto.setMovingAverageInfoDTOS(new ArrayList<>());
             return insufficientDto;
            // 或者抛出异常
            // throw new IllegalArgumentException("数据不足以计算移动平均值，需要至少 " + period + " 天的数据，但只有 " + revenueValues.length + " 天。");
        }

        // 计算收入和利润的移动平均值
        Double[] revenueMovingAverages = MovingAverageCalculator.calculateSimpleMovingAverage(revenueValues, period);
        Double[] profitMovingAverages = MovingAverageCalculator.calculateSimpleMovingAverage(profitValues, period);

        // 构建返回对象
        MovingAverageLineDTO movingAverageLineDTO = new MovingAverageLineDTO();
        movingAverageLineDTO.setDates(localDates);

        // 创建移动平均信息列表
        ArrayList<MovingAverageLineDTO.MovingAverageInfoDTO> movingAverageInfoDTOS = new ArrayList<>();

        // 添加收入移动平均数据
        MovingAverageLineDTO.MovingAverageInfoDTO revenueDTO = new MovingAverageLineDTO.MovingAverageInfoDTO();
        revenueDTO.setName("收入");
        revenueDTO.setData(revenueMovingAverages);
        movingAverageInfoDTOS.add(revenueDTO);

        // 添加利润移动平均数据
        MovingAverageLineDTO.MovingAverageInfoDTO profitDTO = new MovingAverageLineDTO.MovingAverageInfoDTO();
        profitDTO.setName("利润");
        profitDTO.setData(profitMovingAverages);
        movingAverageInfoDTOS.add(profitDTO);

        // 设置移动平均信息列表并返回
        movingAverageLineDTO.setMovingAverageInfoDTOS(movingAverageInfoDTOS);
        return movingAverageLineDTO;
    }


    /**
     * 计算多个产品的移动平均值
     *
     * @param productIds    产品ID数组
     * @param dataExtractor 数据提取器 (根据类型提取利润、销售额或数量)
     * @param period        移动平均周期
     * @param startDate     开始日期
     * @param endDate       结束日期
     * @return 包含各产品移动平均数据的DTO对象
     */
    public MovingAverageLineDTO getMovingAverageForProducts(int[] productIds, DataExtractor dataExtractor, int period, LocalDate startDate, LocalDate endDate) {
        log.info("计算产品移动平均线，产品数: {}, 周期: {}, 日期范围: {} - {}", productIds.length, period, startDate, endDate);

        if (productIds == null || productIds.length == 0) {
            log.warn("产品ID列表为空，无法计算移动平均。");
            MovingAverageLineDTO emptyDto = new MovingAverageLineDTO(); emptyDto.setDates(new ArrayList<>()); emptyDto.setMovingAverageInfoDTOS(new ArrayList<>()); return emptyDto; // 返回空DTO
        }

        // 获取产品ID对应的产品名称 Map
        ProductQuery productQuery = ProductQuery.builder()
                                                .ids(Arrays.stream(productIds).boxed().toList())
                                                .build();
        Map<Integer, String> productNamesMap = productService.findList(productQuery)
                                                             .stream()
                                                             .collect(Collectors.toMap(Product::getId, Product::getName));

        // 获取日期范围内的每日销售统计
        // 注意：这里依赖 TemporalStatisticsService
        Map<LocalDate, SalesStatisticsDTO> dailyStatistics = temporalStatisticsService.calculateDailyStatistics(startDate, endDate);

        if (dailyStatistics.isEmpty()) {
             log.warn("在日期范围 {} 到 {} 内没有找到日统计数据，无法计算产品移动平均。", startDate, endDate);
             MovingAverageLineDTO emptyDto = new MovingAverageLineDTO(); emptyDto.setDates(new ArrayList<>()); emptyDto.setMovingAverageInfoDTOS(new ArrayList<>()); return emptyDto; // 返回空DTO
        }

        // 获取排序后的日期列表
        List<LocalDate> localDates = dailyStatistics.keySet().stream().sorted().toList();

        // 创建移动平均线DTO
        MovingAverageLineDTO movingAverageLineDTO = new MovingAverageLineDTO();
        movingAverageLineDTO.setDates(localDates);
        movingAverageLineDTO.setMovingAverageInfoDTOS(new ArrayList<>()); // 初始化列表

        // 遍历每个产品ID，计算其移动平均
        for (int productId : productIds) {
            String productName = productNamesMap.get(productId);
            if (productName == null) {
                log.warn("未找到 ID 为 {} 的产品名称，跳过该产品的移动平均计算。", productId);
                continue; // 跳过不存在的产品
            }

            // 获取该产品每日销售数据
            double[] dailySalesForProduct = getDailySalesForProduct(dailyStatistics, localDates, productId, dataExtractor);

             // 验证数据量是否足够计算移动平均
            if (dailySalesForProduct.length < period) {
                log.warn("产品 '{}' (ID: {}) 的数据点 ({}) 不足以计算周期为 {} 的移动平均值，跳过计算。",
                         productName, productId, dailySalesForProduct.length, period);
                continue; // 跳过数据不足的产品
            }


            // 计算该产品的简单移动平均
            Double[] movingAverages = MovingAverageCalculator.calculateSimpleMovingAverage(dailySalesForProduct, period);

            // 创建移动平均信息对象并添加到列表中
            MovingAverageLineDTO.MovingAverageInfoDTO movingAverageInfoDTO = new MovingAverageLineDTO.MovingAverageInfoDTO();
            movingAverageInfoDTO.setName(productName); // 使用查到的产品名称
            movingAverageInfoDTO.setData(movingAverages);

            movingAverageLineDTO.getMovingAverageInfoDTOS().add(movingAverageInfoDTO);
        }

        return movingAverageLineDTO;
    }


    /**
     * 从每日统计数据中提取指定商品 ID 的每日指定指标数据（利润、销售额或数量）
     *
     * @param dailyStatistics 每日统计数据 Map<LocalDate, SalesStatisticsDTO>
     * @param sortedDates     已排序的日期列表
     * @param productId       目标商品 ID
     * @param dataExtractor   数据提取器
     * @return 每日指标数据的 double 数组，按日期排序
     */
    private double[] getDailySalesForProduct(Map<LocalDate, SalesStatisticsDTO> dailyStatistics, List<LocalDate> sortedDates, int productId, DataExtractor dataExtractor) {
        // 初始化结果数组，大小与日期数量一致
        double[] dailySales = new double[sortedDates.size()];
        int index = 0;

        // 按排序后的日期遍历
        for (LocalDate date : sortedDates) {
            SalesStatisticsDTO dailyData = dailyStatistics.get(date);
            double sales = 0.0; // 默认为0

            if (dailyData != null && dailyData.getProductSalesInfoDTOS() != null) {
                // 获取当天的商品销量列表
                List<ProductSalesInfoDTO> productSales = dailyData.getProductSalesInfoDTOS();

                // 查找目标商品的销量
                for (ProductSalesInfoDTO product : productSales) {
                    if (product.getProductId() == productId) { // 移除 != null 检查，因为 getProductId() 通常返回 int 或 Integer
                        // 使用 dataExtractor 提取数据
                        BigDecimal extractedValue = dataExtractor.extract(product);
                        sales = (extractedValue != null) ? extractedValue.doubleValue() : 0.0;
                        break; // 找到后即可跳出内层循环
                    }
                }
            }
            // 将销量存入结果数组
            dailySales[index++] = sales;
        }
        return dailySales;
    }

    // TaskType 到 DataExtractor 的映射可以在调用 getMovingAverageForProducts 之前处理好，
    // 或者在这里提供一个辅助方法根据 TaskType 获取 DataExtractor
    public DataExtractor getDataExtractorByType(MovingAverageLineRequest.TaskType taskType) {
        switch (taskType) {
            case Profit:
                return ProductSalesInfoDTO::getTotalProfit;
            case SalesAmount:
                return ProductSalesInfoDTO::getTotalSales;
            case Quantity:
                return productSalesInfoDTO -> BigDecimal.valueOf(productSalesInfoDTO.getQuantity());
            default:
                throw new IllegalArgumentException("不支持的任务类型: " + taskType);
        }
    }

}