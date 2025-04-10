package com.example.domain.statistics.service;

import com.example.domain.order.entity.QOrder;
import com.example.domain.order.entity.QOrderDetail;
import com.example.domain.product.entity.QProduct;
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 时间维度统计服务
 * 提供按日、月、时间范围等时间维度的统计计算服务
 */
@Service
@RequiredArgsConstructor
public class TemporalStatisticsService {

    private final JPAQueryFactory queryFactory;

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
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        NumberTemplate<Integer> yearExpression = Expressions.numberTemplate(Integer.class, "YEAR({0})", qOrder.createTime);
        NumberTemplate<Integer> monthExpression = Expressions.numberTemplate(Integer.class, "MONTH({0})", qOrder.createTime);

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
                .groupBy(yearExpression, monthExpression)
                .orderBy(yearExpression.asc(), monthExpression.asc())
                .fetch();

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
                .groupBy(yearExpression, monthExpression, qProduct.id, qProduct.name)
                .orderBy(yearExpression.asc(), monthExpression.asc(), qProduct.id.asc())
                .fetch();

        Map<YearMonth, SalesStatisticsDTO> monthlyStatsMap = new LinkedHashMap<>();

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
            statsDto.setProductSalesInfoDTOS(new ArrayList<>());
            monthlyStatsMap.put(yearMonth, statsDto);
        }

        for (Tuple row : monthlyProductDetails) {
            Integer year = row.get(yearExpression);
            Integer month = row.get(monthExpression);
            if (year == null || month == null) continue;

            YearMonth yearMonth = YearMonth.of(year, month);
            SalesStatisticsDTO statsDto = monthlyStatsMap.get(yearMonth);
            if (statsDto == null) continue;

            ProductSalesInfoDTO productInfo = new ProductSalesInfoDTO();
            productInfo.setProductId(row.get(qProduct.id));
            productInfo.setProductName(row.get(qProduct.name));
            Number quantitySum = row.get(4, Number.class);
            productInfo.setQuantity(quantitySum != null ? quantitySum.intValue() : 0);
            productInfo.setTotalSales(row.get(5, BigDecimal.class));
            productInfo.setTotalProfit(row.get(6, BigDecimal.class));

            statsDto.getProductSalesInfoDTOS().add(productInfo);
        }

        Map<YearMonth, SalesStatisticsDTO> finalResultMap = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);

        while (!currentMonth.isAfter(endMonth)) {
            finalResultMap.put(currentMonth, monthlyStatsMap.getOrDefault(currentMonth, createEmptySalesStatistics()));
            currentMonth = currentMonth.plusMonths(1);
        }

        return finalResultMap;
    }

    /**
     * 计算指定日期范围内的总体销售统计数据
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return SalesStatisticsDTO 包含日期范围内总体统计和按商品分组的统计
     */
    public SalesStatisticsDTO calculateDateRangeStatistics(LocalDate startDate, LocalDate endDate) {
        QOrder qOrder = QOrder.order;
        QOrderDetail qOrderDetail = QOrderDetail.orderDetail;
        QProduct qProduct = QProduct.product;

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Tuple overallStats = queryFactory
                .select(
                        qOrder.id.count(),
                        qOrder.totalSalesAmount.sum().coalesce(BigDecimal.ZERO),
                        qOrder.totalProfit.sum().coalesce(BigDecimal.ZERO)
                )
                .from(qOrder)
                .where(qOrder.createTime.goe(startDateTime).and(qOrder.createTime.lt(endDateTime)))
                .fetchOne();

        SalesStatisticsDTO result = new SalesStatisticsDTO();
        if (overallStats != null) {
            result.setOrderCount(overallStats.get(0, Long.class).intValue());
            result.setTotalSales(overallStats.get(1, BigDecimal.class));
            result.setTotalProfit(overallStats.get(2, BigDecimal.class));
            result.setTotalCost(result.getTotalSales().subtract(result.getTotalProfit()));
        } else {
            result = createEmptySalesStatistics(); // 使用辅助方法创建空对象
            return result;
        }

        List<ProductSalesInfoDTO> productStats = queryFactory
                .select(Projections.constructor(ProductSalesInfoDTO.class,
                        qProduct.id,
                        qProduct.name,
                        qOrderDetail.quantity.sum().coalesce(0),
                        qOrderDetail.totalSalesAmount.sum().coalesce(BigDecimal.ZERO),
                        qOrderDetail.totalProfit.sum().coalesce(BigDecimal.ZERO)
                ))
                .from(qOrderDetail)
                .join(qOrderDetail.order, qOrder)
                .join(qOrderDetail.product, qProduct)
                .where(qOrder.createTime.goe(startDateTime).and(qOrder.createTime.lt(endDateTime)))
                .groupBy(qProduct.id, qProduct.name)
                .orderBy(qProduct.id.asc())
                .fetch();

        result.setProductSalesInfoDTOS(productStats);

        return result;
    }

    /**
     * 计算指定日期范围内每日的销售统计数据
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return Map<LocalDate, SalesStatisticsDTO> 按日期分组的统计数据
     */
    public Map<LocalDate, SalesStatisticsDTO> calculateDailyStatistics(LocalDate startDate, LocalDate endDate) {
        QOrder qOrder = QOrder.order;
        QOrderDetail qOrderDetail = QOrderDetail.orderDetail;
        QProduct qProduct = QProduct.product;

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        DateTemplate<java.sql.Date> orderSqlDate = Expressions.dateTemplate(java.sql.Date.class, "DATE({0})", qOrder.createTime);

        List<Tuple> dailyTotals = queryFactory
                .select(
                        orderSqlDate,
                        qOrder.id.countDistinct(),
                        qOrder.totalSalesAmount.sum().coalesce(BigDecimal.ZERO),
                        qOrder.totalProfit.sum().coalesce(BigDecimal.ZERO)
                )
                .from(qOrder)
                .where(qOrder.createTime.goe(startDateTime).and(qOrder.createTime.lt(endDateTime)))
                .groupBy(orderSqlDate)
                .orderBy(orderSqlDate.asc())
                .fetch();

        Map<LocalDate, SalesStatisticsDTO> dailyStatsMap = new LinkedHashMap<>();
        for (Tuple row : dailyTotals) {
            java.sql.Date sqlDate = row.get(orderSqlDate);
            if (sqlDate == null) continue;
            LocalDate date = sqlDate.toLocalDate();

            SalesStatisticsDTO statsDto = new SalesStatisticsDTO();
            statsDto.setOrderCount(row.get(1, Long.class).intValue());
            statsDto.setTotalSales(row.get(2, BigDecimal.class));
            statsDto.setTotalProfit(row.get(3, BigDecimal.class));
            statsDto.setTotalCost(statsDto.getTotalSales().subtract(statsDto.getTotalProfit()));
            statsDto.setProductSalesInfoDTOS(new ArrayList<>());
            dailyStatsMap.put(date, statsDto);
        }

        List<Tuple> dailyProductDetails = queryFactory
                .select(
                        orderSqlDate,
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
                .groupBy(orderSqlDate, qProduct.id, qProduct.name)
                .orderBy(orderSqlDate.asc(), qProduct.id.asc())
                .fetch();

        for (Tuple row : dailyProductDetails) {
            java.sql.Date sqlDate = row.get(orderSqlDate);
            if (sqlDate == null) continue;
            LocalDate date = sqlDate.toLocalDate();

            SalesStatisticsDTO statsDto = dailyStatsMap.get(date);
            if (statsDto == null) continue;

            ProductSalesInfoDTO productInfo = new ProductSalesInfoDTO();
            productInfo.setProductId(row.get(qProduct.id));
            productInfo.setProductName(row.get(qProduct.name));
            Number quantitySum = row.get(3, Number.class);
            productInfo.setQuantity(quantitySum != null ? quantitySum.intValue() : 0);
            productInfo.setTotalSales(row.get(4, BigDecimal.class));
            productInfo.setTotalProfit(row.get(5, BigDecimal.class));

            statsDto.getProductSalesInfoDTOS().add(productInfo);
        }

        Map<LocalDate, SalesStatisticsDTO> finalResultMap = new LinkedHashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            finalResultMap.put(currentDate, dailyStatsMap.getOrDefault(currentDate, createEmptySalesStatistics()));
            currentDate = currentDate.plusDays(1);
        }

        return finalResultMap;
    }

    // 创建一个空的 SalesStatisticsDTO 用于填充无销售的日期/月份
    private SalesStatisticsDTO createEmptySalesStatistics() {
        SalesStatisticsDTO emptyStats = new SalesStatisticsDTO();
        emptyStats.setOrderCount(0);
        emptyStats.setTotalSales(BigDecimal.ZERO);
        emptyStats.setTotalProfit(BigDecimal.ZERO);
        emptyStats.setTotalCost(BigDecimal.ZERO);
        emptyStats.setProductSalesInfoDTOS(new ArrayList<>());
        return emptyStats;
    }
}