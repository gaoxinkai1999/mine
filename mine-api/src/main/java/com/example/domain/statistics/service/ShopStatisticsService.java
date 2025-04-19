package com.example.domain.statistics.service;

import com.example.domain.order.entity.QOrder;
import com.example.domain.order.entity.QOrderDetail;
import com.example.domain.product.entity.QProduct;
import com.example.domain.shop.entity.Shop;
import com.example.domain.shop.service.ShopService;
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;
import com.example.domain.statistics.dto.response.ShopStatisticsDTO;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商家统计服务
 * 提供商家维度的统计数据计算服务
 */
@Service
@RequiredArgsConstructor // 使用 Lombok 自动注入 final 字段
public class ShopStatisticsService {

    private final ShopService shopService; // 商家模块服务
    private final JPAQueryFactory queryFactory;

    /**
     * 计算所有商家的统计数据
     *
     * @return List<ShopStatisticsDTO> 包含所有商家统计结果的列表
     */
    public List<ShopStatisticsDTO> calculateShopStatistics() {
        // 查询全部未删除商家
        List<Shop> shops = shopService.findActiveShopsQueryDSL(90);
        Map<Integer, Shop> shopMap = shops.stream()
                .collect(Collectors.toMap(Shop::getId, Function.identity()));

        if (shopMap.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, ShopStatisticsDTO> shopStats = getShopAggregateStatistics(shopMap);
        Map<Integer, List<ProductSalesInfoDTO>> productStats = getShopProductMonthlyStatistics(shopMap);

        // 填充没有销售数据的商家
        for (Integer shopId : shopMap.keySet()) {
            shopStats.computeIfAbsent(shopId, id -> {
                Shop shop = shopMap.get(id);
                ShopStatisticsDTO dto = new ShopStatisticsDTO();
                dto.setShopId(id);
                dto.setShopName(shop.getName());
                dto.setLocation(shop.getLocation());
                dto.setArrears(shop.getArrears());
                dto.setTotalSales(BigDecimal.ZERO);
                dto.setTotalProfit(BigDecimal.ZERO);
                dto.setAverageMonthlyProfit(BigDecimal.ZERO);
                return dto;
            });
        }


        for (Map.Entry<Integer, ShopStatisticsDTO> entry : shopStats.entrySet()) {
            int shopId = entry.getKey();
            ShopStatisticsDTO dto = entry.getValue();
            dto.setProductMonthlySalesList(productStats.getOrDefault(shopId, Collections.emptyList()));
        }
        return new ArrayList<>(shopStats.values());
    }

    /**
     * 查询每个商家的总销售额、总利润，并计算平均月利润
     */
    private Map<Integer, ShopStatisticsDTO> getShopAggregateStatistics(Map<Integer, Shop> shopMap) {
        QOrder qOrder = QOrder.order;

        List<Tuple> results = queryFactory
            .select(
                qOrder.shop.id,
                qOrder.totalSalesAmount.sum().coalesce(BigDecimal.ZERO),
                qOrder.totalProfit.sum().coalesce(BigDecimal.ZERO)
            )
            .from(qOrder)
            .where(qOrder.shop.id.in(shopMap.keySet())) // 仅查询存在的商家
            .groupBy(qOrder.shop.id)
            .fetch();

        Map<Integer, ShopStatisticsDTO> map = new HashMap<>();
        for (Tuple t : results) {
            Integer shopId = t.get(0, Integer.class);
            // shopMap.containsKey(shopId) 在 where 条件中已保证
            Shop shop = shopMap.get(shopId);

            BigDecimal totalSales = t.get(1, BigDecimal.class);
            BigDecimal totalProfit = t.get(2, BigDecimal.class);

            long days = ChronoUnit.DAYS.between(shop.getCreateTime(), LocalDate.now());
            // 避免除零或负数，至少算一个月
            BigDecimal months = days > 0 ? BigDecimal.valueOf(days).divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP) : BigDecimal.ONE;

            ShopStatisticsDTO dto = new ShopStatisticsDTO();
            dto.setShopId(shopId);
            dto.setShopName(shop.getName());
            dto.setLocation(shop.getLocation());
            dto.setArrears(shop.getArrears());
            dto.setTotalSales(totalSales);
            dto.setTotalProfit(totalProfit);
            dto.setAverageMonthlyProfit(
                months.compareTo(BigDecimal.ZERO) > 0 ?
                totalProfit.divide(months, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO
            );
            map.put(shopId, dto);
        }
        return map;
    }

    /**
     * 查询每个商家下每个商品的总销量、销售额、利润，并按上线天数计算平均月数据
     */
    private Map<Integer, List<ProductSalesInfoDTO>> getShopProductMonthlyStatistics(Map<Integer, Shop> shopMap) {
        QOrder qOrder = QOrder.order;
        QOrderDetail qOrderDetail = QOrderDetail.orderDetail;
        QProduct qProduct = QProduct.product;

        List<Tuple> results = queryFactory
            .select(
                qOrder.shop.id,
                qOrderDetail.product.id,
                qProduct.name,
                qOrderDetail.quantity.sum().coalesce(0),
                qOrderDetail.totalSalesAmount.sum().coalesce(BigDecimal.ZERO),
                qOrderDetail.totalProfit.sum().coalesce(BigDecimal.ZERO)
            )
            .from(qOrderDetail)
            .join(qOrderDetail.order, qOrder)
            .join(qOrderDetail.product, qProduct)
            .where(qOrder.shop.id.in(shopMap.keySet())) // 仅查询存在的商家
            .groupBy(qOrder.shop.id, qOrderDetail.product.id, qProduct.name)
            .fetch();

        Map<Integer, List<ProductSalesInfoDTO>> resultMap = new HashMap<>();
        for (Tuple t : results) {
            Integer shopId = t.get(0, Integer.class);
            // shopMap.containsKey(shopId) 在 where 条件中已保证
            Shop shop = shopMap.get(shopId);

            Integer productId = t.get(1, Integer.class);
            String productName = t.get(2, String.class);
            Integer totalQuantity = t.get(3, Integer.class);
            BigDecimal totalSales = t.get(4, BigDecimal.class);
            BigDecimal totalProfit = t.get(5, BigDecimal.class);

            long days = ChronoUnit.DAYS.between(shop.getCreateTime(), LocalDate.now());
            BigDecimal months = days > 0 ? BigDecimal.valueOf(days).divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP) : BigDecimal.ONE;

            ProductSalesInfoDTO dto = new ProductSalesInfoDTO();
            dto.setProductId(productId);
            dto.setProductName(productName);
            // 计算月均销量
            dto.setQuantity(
                months.compareTo(BigDecimal.ZERO) > 0 ?
                BigDecimal.valueOf(totalQuantity).divide(months, 0, RoundingMode.HALF_UP).intValue() :
                0
            );
             // 计算月均销售额
            dto.setTotalSales(
                months.compareTo(BigDecimal.ZERO) > 0 ?
                totalSales.divide(months, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO
            );
            // 计算月均利润
            dto.setTotalProfit(
                months.compareTo(BigDecimal.ZERO) > 0 ?
                totalProfit.divide(months, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO
            );

            resultMap.computeIfAbsent(shopId, k -> new ArrayList<>()).add(dto);
        }
        return resultMap;
    }
}