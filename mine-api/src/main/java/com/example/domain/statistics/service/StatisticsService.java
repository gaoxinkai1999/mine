package com.example.domain.statistics.service;

import com.example.domain.order.entity.Order;
import com.example.domain.order.entity.QOrder;
import com.example.domain.order.entity.QOrderDetail;
import com.example.domain.order.service.OrderService;
import com.example.domain.product.entity.Product;
import com.example.domain.product.entity.QProduct;
import com.example.domain.product.service.ProductService;
import com.example.domain.shop.entity.Shop;
import com.example.domain.shop.service.ShopService;
import com.example.domain.statistics.dto.request.MovingAverageLineRequest;
import com.example.domain.statistics.dto.response.MovingAverageLineDTO;
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.domain.statistics.dto.response.ShopStatisticsDTO;
import com.example.query.OrderQuery;
import com.example.query.ProductQuery;
import com.example.query.ShopQuery;
import com.example.utils.DataExtractor;
import com.example.utils.MovingAverageCalculator;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 统计服务类
 * 提供各类统计数据的计算服务，包括商家统计、日期范围统计等
 */
@Service
@Slf4j
public class StatisticsService {

    @Autowired
    private OrderService orderService; // 订单模块服务

    @Autowired
    private ShopService shopService; // 商家模块服务

    @Autowired
    private ProductService productService; // 商品模块服务
    @Autowired
    private JPAQueryFactory queryFactory;


    /**
     * 计算所有商家的统计数据
     *
     * @return List<ShopStatisticsDTO> 包含所有商家统计结果的列表
     */
    public List<ShopStatisticsDTO> calculateShopStatistics() {
        ShopQuery shopQuery = ShopQuery.builder()
                                       .isDel(false)
                                       .build();

        List<Shop> shops = shopService.findList(shopQuery);

        OrderQuery orderQuery = OrderQuery.builder()
                                          .includes(OrderQuery.Include.WITH_SHOP)
                                          .build();

        // 获取所有订单
        List<Order> orders = orderService.findList(orderQuery);

        // 对每个商家计算统计结果
        return shops.stream()
                    .map(shop -> calculateStatisticsForShop(shop, orders))
                    .collect(Collectors.toList());
    }



    /**
     * 计算指定日期范围内每个月的销售统计数据（优化版）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return Map<YearMonth, SalesStatisticsDTO> 按年月分组的统计数据
     */
    public Map<YearMonth, SalesStatisticsDTO> calculateMonthlyStatisticsOptimized(LocalDate startDate, LocalDate endDate) {
        QOrder qOrder = QOrder.order;
        QOrderDetail qOrderDetail = QOrderDetail.orderDetail;
        QProduct qProduct = QProduct.product;

        LocalDateTime startDateTime = startDate.atStartOfDay();
        // 结束时间通常应包含 endDate 当天，所以用 lt(endDate + 1 day)
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // --- 定义用于分组的 Year 和 Month 表达式 ---
        // 根据你的数据库调整 YEAR() 和 MONTH() 函数
        NumberTemplate<Integer> yearExpression = Expressions.numberTemplate(Integer.class, "YEAR({0})", qOrder.createTime);
        NumberTemplate<Integer> monthExpression = Expressions.numberTemplate(Integer.class, "MONTH({0})", qOrder.createTime);

        // --- 查询 1: 获取每月总体统计 ---
        List<Tuple> monthlyTotals = queryFactory
                .select(
                        yearExpression,
                        monthExpression,
                        qOrder.id.countDistinct(),
                        qOrder.totalSalesAmount.sum().coalesce(BigDecimal.ZERO),
                        qOrder.totalProfit.sum().coalesce(BigDecimal.ZERO)
                )
                .from(qOrder)
                .where(qOrder.createTime.goe(startDateTime).and(qOrder.createTime.lt(endDateTime)))
                .groupBy(yearExpression, monthExpression) // 按年和月分组
                .orderBy(yearExpression.asc(), monthExpression.asc()) // 按年月排序
                .fetch();

        // --- 查询 2: 获取每月每商品详细统计 ---
        List<Tuple> monthlyProductDetails = queryFactory
                .select(
                        yearExpression,
                        monthExpression,
                        qProduct.id,
                        qProduct.name,
                        qOrderDetail.quantity.sum().coalesce(0),
                        qOrderDetail.totalSalesAmount.sum().coalesce(BigDecimal.ZERO),
                        qOrderDetail.totalProfit.sum().coalesce(BigDecimal.ZERO)
                )
                .from(qOrderDetail)
                .join(qOrderDetail.order, qOrder)
                .join(qOrderDetail.product, qProduct)
                .where(qOrder.createTime.goe(startDateTime).and(qOrder.createTime.lt(endDateTime)))
                .groupBy(yearExpression, monthExpression, qProduct.id, qProduct.name) // 按年月和产品分组
                .orderBy(yearExpression.asc(), monthExpression.asc(), qProduct.id.asc())
                .fetch();

        // --- 合并结果到 Map<YearMonth, SalesStatisticsDTO> ---
        Map<YearMonth, SalesStatisticsDTO> monthlyStatsMap = new LinkedHashMap<>();

        // 处理总体统计
        for (Tuple row : monthlyTotals) {
            Integer year = row.get(yearExpression);
            Integer month = row.get(monthExpression);
            if (year == null || month == null) continue;

            YearMonth yearMonth = YearMonth.of(year, month);
            SalesStatisticsDTO statsDto = new SalesStatisticsDTO();
            statsDto.setOrderCount(row.get(2, Long.class).intValue());
            statsDto.setTotalSales(row.get(3, BigDecimal.class));
            statsDto.setTotalProfit(row.get(4, BigDecimal.class));
            statsDto.setTotalCost(statsDto.getTotalSales().subtract(statsDto.getTotalProfit()));
            statsDto.setProductSalesInfoDTOS(new ArrayList<>()); // 初始化产品列表
            monthlyStatsMap.put(yearMonth, statsDto);
        }

        // 处理产品详情统计
        for (Tuple row : monthlyProductDetails) {
            Integer year = row.get(yearExpression);
            Integer month = row.get(monthExpression);
            if (year == null || month == null) continue;

            YearMonth yearMonth = YearMonth.of(year, month);
            SalesStatisticsDTO statsDto = monthlyStatsMap.get(yearMonth);
            // 如果某个月份只有详情而没有总体统计（理论上不可能），则跳过
            if (statsDto == null) continue;

            ProductSalesInfoDTO productInfo = new ProductSalesInfoDTO();
            productInfo.setProductId(row.get(qProduct.id));
            productInfo.setProductName(row.get(qProduct.name));
            Number quantitySum = row.get(4, Number.class); // 获取为 Number
            productInfo.setQuantity(quantitySum != null ? quantitySum.intValue() : 0); // 转换为 int
            productInfo.setTotalSales(row.get(5, BigDecimal.class));
            productInfo.setTotalProfit(row.get(6, BigDecimal.class));

            statsDto.getProductSalesInfoDTOS().add(productInfo);
        }

        // --- 填充缺失的月份 ---
        Map<YearMonth, SalesStatisticsDTO> finalResultMap = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);

        while (!currentMonth.isAfter(endMonth)) {
            finalResultMap.put(currentMonth, monthlyStatsMap.getOrDefault(currentMonth, createEmptySalesStatistics()));
            currentMonth = currentMonth.plusMonths(1);
        }

        return finalResultMap;
    }

    public SalesStatisticsDTO calculateDateRangeStatistics(LocalDate startDate, LocalDate endDate) {
        QOrder qOrder = QOrder.order;
        QOrderDetail qOrderDetail = QOrderDetail.orderDetail;
        QProduct qProduct = QProduct.product;

        // 定义时间范围
        LocalDateTime startDateTime = startDate.atStartOfDay();
        // 结束时间需要包含当天的最后一秒，或者查询时用 < 次日开始时间
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // 1. 查询总体统计数据 (订单数, 总销售额, 总利润)
        Tuple overallStats = queryFactory
                .select(
                        qOrder.id.count(),
                        qOrder.totalSalesAmount.sum().coalesce(BigDecimal.ZERO), // coalesce 处理可能为null的SUM结果
                        qOrder.totalProfit.sum().coalesce(BigDecimal.ZERO)
                )
                .from(qOrder)
                .where(qOrder.createTime.goe(startDateTime).and(qOrder.createTime.lt(endDateTime))) // 使用 >= start 和 < end+1天
                .fetchOne(); // 预期只有一行结果

        SalesStatisticsDTO result = new SalesStatisticsDTO();
        if (overallStats != null) {
            result.setOrderCount(overallStats.get(0, Long.class).intValue()); // count返回Long
            result.setTotalSales(overallStats.get(1, BigDecimal.class));
            result.setTotalProfit(overallStats.get(2, BigDecimal.class));
            // 计算总成本
            result.setTotalCost(result.getTotalSales().subtract(result.getTotalProfit()));
        } else {
            // 如果没有订单，则所有统计数据为0
            result.setOrderCount(0);
            result.setTotalSales(BigDecimal.ZERO);
            result.setTotalProfit(BigDecimal.ZERO);
            result.setTotalCost(BigDecimal.ZERO);
            result.setProductSalesInfoDTOS(new ArrayList<>());
            return result; // 如果没有订单，直接返回0值DTO
        }


        // 2. 查询按商品分组的统计数据 (商品ID, 名称, 数量, 销售额, 利润)
        List<ProductSalesInfoDTO> productStats = queryFactory
                .select(Projections.constructor(ProductSalesInfoDTO.class, // 使用构造函数投影到DTO
                        qProduct.id,
                        qProduct.name,
                        qOrderDetail.quantity.sum().coalesce(0), // 数量总和
                        qOrderDetail.totalSalesAmount.sum().coalesce(BigDecimal.ZERO), // 销售额总和
                        qOrderDetail.totalProfit.sum().coalesce(BigDecimal.ZERO) // 利润总和
                ))
                .from(qOrderDetail)
                .join(qOrderDetail.order, qOrder) // 关联订单以过滤时间
                .join(qOrderDetail.product, qProduct) // 关联产品以获取名称和ID
                .where(qOrder.createTime.goe(startDateTime).and(qOrder.createTime.lt(endDateTime)))
                .groupBy(qProduct.id, qProduct.name) // 按产品ID和名称分组
                .orderBy(qProduct.id.asc()) // 可选排序
                .fetch();

        result.setProductSalesInfoDTOS(productStats);

        return result;
    }
    public Map<LocalDate, SalesStatisticsDTO> calculateDailyStatistics(LocalDate startDate, LocalDate endDate) {
        QOrder qOrder = QOrder.order;
        QOrderDetail qOrderDetail = QOrderDetail.orderDetail;
        QProduct qProduct = QProduct.product;

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // 修改1: 期望接收 java.sql.Date
        DateTemplate<java.sql.Date> orderSqlDate = Expressions.dateTemplate(java.sql.Date.class, "DATE({0})", qOrder.createTime);

        // --- 查询 1: 获取每日总体统计 ---
        List<Tuple> dailyTotals = queryFactory
                .select(
                        orderSqlDate, // 使用新的 DateTemplate
                        qOrder.id.countDistinct(),
                        qOrder.totalSalesAmount.sum().coalesce(BigDecimal.ZERO),
                        qOrder.totalProfit.sum().coalesce(BigDecimal.ZERO)
                )
                .from(qOrder)
                .where(qOrder.createTime.goe(startDateTime).and(qOrder.createTime.lt(endDateTime)))
                .groupBy(orderSqlDate) // 按 sql.Date 分组
                .orderBy(orderSqlDate.asc())
                .fetch();

        Map<LocalDate, SalesStatisticsDTO> dailyStatsMap = new LinkedHashMap<>();
        for (Tuple row : dailyTotals) {
            // 修改2: 获取 java.sql.Date 并转换为 java.time.LocalDate
            java.sql.Date sqlDate = row.get(orderSqlDate);
            if (sqlDate == null) continue;
            LocalDate date = sqlDate.toLocalDate(); // 转换

            SalesStatisticsDTO statsDto = new SalesStatisticsDTO();
            statsDto.setOrderCount(row.get(1, Long.class).intValue());
            statsDto.setTotalSales(row.get(2, BigDecimal.class));
            statsDto.setTotalProfit(row.get(3, BigDecimal.class));
            statsDto.setTotalCost(statsDto.getTotalSales().subtract(statsDto.getTotalProfit()));
            statsDto.setProductSalesInfoDTOS(new ArrayList<>());
            dailyStatsMap.put(date, statsDto);
        }

        // --- 查询 2: 获取每日每商品详细统计 ---
        List<Tuple> dailyProductDetails = queryFactory
                .select(
                        orderSqlDate, // 使用新的 DateTemplate
                        qProduct.id,
                        qProduct.name,
                        qOrderDetail.quantity.sum().coalesce(0),
                        qOrderDetail.totalSalesAmount.sum().coalesce(BigDecimal.ZERO),
                        qOrderDetail.totalProfit.sum().coalesce(BigDecimal.ZERO)
                )
                .from(qOrderDetail)
                .join(qOrderDetail.order, qOrder)
                .join(qOrderDetail.product, qProduct)
                .where(qOrder.createTime.goe(startDateTime).and(qOrder.createTime.lt(endDateTime)))
                .groupBy(orderSqlDate, qProduct.id, qProduct.name) // 按 sql.Date 和产品分组
                .orderBy(orderSqlDate.asc(), qProduct.id.asc())
                .fetch();

        // --- 合并结果 ---
        for (Tuple row : dailyProductDetails) {
            // 修改2: 获取 java.sql.Date 并转换为 java.time.LocalDate
            java.sql.Date sqlDate = row.get(orderSqlDate);
            if (sqlDate == null) continue;
            LocalDate date = sqlDate.toLocalDate(); // 转换

            SalesStatisticsDTO statsDto = dailyStatsMap.get(date);
            if (statsDto == null) continue;

            ProductSalesInfoDTO productInfo = new ProductSalesInfoDTO();
            productInfo.setProductId(row.get(qProduct.id));
            productInfo.setProductName(row.get(qProduct.name));
            // 处理可能的类型转换 (sum 可能返回 Long 或 Integer)
            Number quantitySum = row.get(3, Number.class);
            productInfo.setQuantity(quantitySum != null ? quantitySum.intValue() : 0);
            productInfo.setTotalSales(row.get(4, BigDecimal.class));
            productInfo.setTotalProfit(row.get(5, BigDecimal.class));

            statsDto.getProductSalesInfoDTOS().add(productInfo);
        }

        // --- 填充缺失日期 ---
        Map<LocalDate, SalesStatisticsDTO> finalResultMap = new LinkedHashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            finalResultMap.put(currentDate, dailyStatsMap.getOrDefault(currentDate, createEmptySalesStatistics()));
            currentDate = currentDate.plusDays(1);
        }

        return finalResultMap;
    }

    // 创建一个空的 SalesStatisticsDTO 用于填充无销售的日期
    private SalesStatisticsDTO createEmptySalesStatistics() {
        SalesStatisticsDTO emptyStats = new SalesStatisticsDTO();
        emptyStats.setOrderCount(0);
        emptyStats.setTotalSales(BigDecimal.ZERO);
        emptyStats.setTotalProfit(BigDecimal.ZERO);
        emptyStats.setTotalCost(BigDecimal.ZERO);
        emptyStats.setProductSalesInfoDTOS(new ArrayList<>());
        return emptyStats;
    }



    /**
     * 计算单个商家的统计信息
     *
     * @param shop   商家信息
     * @param orders 所有订单列表
     * @return ShopStatisticsDTO 单个商家的统计结果
     */
    private ShopStatisticsDTO calculateStatisticsForShop(Shop shop, List<Order> orders) {
        // 过滤出该商家的订单
        List<Order> shopOrders = orders.stream()
                                       .filter(order -> order.getShop()
                                                             .getId() == shop.getId())
                                       .toList();
        // 计算总销售额和总利润
        BigDecimal totalSales = shopOrders.stream()
                                          .map(Order::getTotalSalesAmount)
                                          .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfit = shopOrders.stream()
                                           .map(Order::getTotalProfit)
                                           .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算平均月利润
        LocalDate now = LocalDate.now(); // 当前日期
        long monthsSinceLaunch = ChronoUnit.MONTHS.between(shop.getCreateTime(), now); // 上架以来的月数

        BigDecimal averageMonthlyProfit = monthsSinceLaunch > 0 ? totalProfit.divide(BigDecimal.valueOf(monthsSinceLaunch), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // 封装统计结果
        ShopStatisticsDTO result = new ShopStatisticsDTO();
        result.setShopId(shop.getId());
        result.setShopName(shop.getName());
        result.setTotalSales(totalSales);
        result.setTotalProfit(totalProfit);
        result.setAverageMonthlyProfit(averageMonthlyProfit);

        return result;
    }



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

        if (orders == null || orders.isEmpty()) {
            throw new RuntimeException("没有找到订单记录");
        }

        // 按日期汇总订单的收入和利润
        for (Order order : orders) {
            LocalDate date = order.getCreateTime()
                                  .toLocalDate();
            // 使用Order中已有的totalSalesAmount和totalProfit字段
            totalRevenueMap.merge(date, order.getTotalSalesAmount(), BigDecimal::add);
            totalProfitMap.merge(date, order.getTotalProfit(), BigDecimal::add);
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
            throw new IllegalArgumentException("数据不足以计算移动平均值");
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
     * @param productIds 产品ID数组
     * @return 移动平均线DTO列表
     */

    public MovingAverageLineDTO getMovingAverage(int[] productIds, DataExtractor dataExtractor, int period) {
        log.info("计算移动平均线，产品数: {}, 周期: {}", productIds.length, period);
        // 获取产品ID对应的产品名称
        ProductQuery productQuery = ProductQuery.builder()
                                                .ids(Arrays.stream(productIds)
                                                           .boxed()
                                                           .toList())
                                                .build();
        List<String> namesByIds = productService.findList(productQuery)
                                                .stream()
                                                .map(Product::getName)
                                                .toList();


        // 设置日期范围（2024年11月10日至2025年1月9日）
        LocalDate startDate = LocalDate.of(2024, 2, 20);
        LocalDate endDate = LocalDate.now();

        // 获取日期范围内的每日销售统计
        Map<LocalDate, SalesStatisticsDTO> dailyStatistics = calculateDailyStatistics(startDate, endDate);
        // 获取每日销售统计的日期列表
        List<LocalDate> localDates = dailyStatistics.keySet()
                                                    .stream()
                                                    .sorted()
                                                    .toList();
        // 创建移动平均线DTO
        MovingAverageLineDTO movingAverageLineDTO = new MovingAverageLineDTO();
        movingAverageLineDTO.setDates(localDates);

        // 遍历每个产品ID，计算其移动平均
        for (int i = 0; i < productIds.length; i++) {
            int productId = productIds[i];


            // 获取该产品每日销售数据
            double[] dailySalesForProduct = getDailySalesForProduct(dailyStatistics, productId, dataExtractor);


            // 计算该产品的简单移动平均
            Double[] movingAverages = MovingAverageCalculator.calculateSimpleMovingAverage(dailySalesForProduct, period);

            // 创建移动平均信息对象并添加到列表中
            MovingAverageLineDTO.MovingAverageInfoDTO movingAverageInfoDTO = new MovingAverageLineDTO.MovingAverageInfoDTO();
            movingAverageInfoDTO.setName(namesByIds.get(i));
            movingAverageInfoDTO.setData(movingAverages);

            movingAverageLineDTO.getMovingAverageInfoDTOS()
                                .add(movingAverageInfoDTO);


        }


        return movingAverageLineDTO;
    }


    private static final Map<MovingAverageLineRequest.TaskType, Function<ProductSalesInfoDTO, BigDecimal>> taskMap = new HashMap<>();

    static {
        // 任务映射
        taskMap.put(MovingAverageLineRequest.TaskType.Profit, ProductSalesInfoDTO::getTotalProfit);
        taskMap.put(MovingAverageLineRequest.TaskType.SalesAmount, ProductSalesInfoDTO::getTotalSales);
        taskMap.put(MovingAverageLineRequest.TaskType.Quantity, productSalesInfoDTO -> BigDecimal.valueOf(productSalesInfoDTO.getQuantity()));
    }

    /**
     * 从数据结构中提取指定商品 ID 的每日销量
     *
     * @param data      Map<LocalDate, DateRangeStatisticsDTO> 数据结构
     * @param productId 目标商品 ID
     * @return 每日销量的 double 数组
     */
    private double[] getDailySalesForProduct(Map<LocalDate, SalesStatisticsDTO> data, int productId, DataExtractor dataExtractor) {
        // 初始化结果数组
        double[] dailySales = new double[data.size()];
        int index = 0;

        // 按日期顺序遍历数据
        List<LocalDate> sortedDates = new ArrayList<>(data.keySet());
        Collections.sort(sortedDates); // 按日期排序

        for (LocalDate date : sortedDates) {
            SalesStatisticsDTO dailyData = data.get(date);

            // 获取当天的商品销量列表
            List<ProductSalesInfoDTO> productQuantities = dailyData.getProductSalesInfoDTOS();

            // 查找目标商品的销量
            double sales = 0;
            for (ProductSalesInfoDTO product : productQuantities) {
                if (product.getProductId() == productId) {

                    sales = dataExtractor.extract(product)
                                         .doubleValue(); // 转换为 double
                    break;
                }
            }

            // 将销量存入结果数组
            dailySales[index] = sales;
            index++;  // 更清晰，易于理解

        }

        return dailySales;
    }


}

