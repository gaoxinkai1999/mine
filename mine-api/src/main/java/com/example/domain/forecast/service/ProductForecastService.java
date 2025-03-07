package com.example.domain.forecast.service;

import com.example.domain.forecast.model.ProductCategory;
import com.example.domain.forecast.model.ProductForecast;
import com.example.domain.forecast.model.ProductSales;
import com.example.domain.forecast.model.ProductWindowData;
import com.example.domain.product.entity.Product;
import com.example.domain.product.service.ProductService;
import com.example.query.ProductQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品预测服务
 * 提供商品级销售预测功能
 */
@Service
@Slf4j
public class ProductForecastService {

    private static final int SCALE = 2; // 小数位数

    @Autowired
    private ProductService productService;
    
    @Autowired
    private WindowAnalysisService windowAnalysisService;

    /**
     * 获取当前在售商品ID列表
     *
     * @return 在售商品ID集合
     */
    public Set<Integer> getActiveProductIds() {
        ProductQuery query = ProductQuery.builder()
                                         .isDel(false)
                                         .build();
                
        List<Product> productList = productService.findList(query);
        
        log.info("获取在售商品ID，共找到 {} 个在售商品", productList.size());
        
        return productList.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 基于滑动窗口的产品级预测生成，只预测在售商品
     *
     * @param productSales 商品销售数据
     * @param windowSize 窗口大小(天)
     * @param activeProductIds 在售商品ID集合
     * @return 商品预测结果映射
     */
    public Map<String, ProductForecast> generateProductForecastsFromWindows(
            List<ProductSales> productSales,
            int windowSize,
            Set<Integer> activeProductIds) {

        if (productSales == null || productSales.isEmpty()) {
            log.warn("没有产品销售数据可用于预测");
            return Collections.emptyMap();
        }

        log.info("开始生成产品级预测，基于 {} 条产品销售记录，窗口大小: {} 天", 
                productSales.size(), windowSize);

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
            log.warn("无法确定销售数据的全局时间范围");
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
            List<ProductWindowData> productWindows = windowAnalysisService.createProductWindows(
                    productHistory, windowSize, globalStartDate, globalEndDate);

            if (productWindows.isEmpty()) {
                log.warn("产品ID {} 没有有效的窗口数据", productId);
                continue;
            }

            // 获取产品信息
            String productName = productHistory.get(0).getProductName();
            log.info("为产品「{}」生成预测", productName);

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
                log.debug("产品「{}」在最后窗口没有销售记录，查找前一窗口销量", productName);

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
                .category(category.name())
                .build();

            forecasts.put(productName, forecast);
            
            log.debug("产品「{}」预测结果：上期销量={}, 预测销量={}, 增长率={}, 销售额={}, 利润={}",
                productName, lastWindowSales, predictedSales, 
                BigDecimal.valueOf(growthRate).setScale(4, RoundingMode.HALF_UP),
                salesAmount, profitAmount);
        }

        log.info("产品级预测完成：处理 {} 个产品，生成 {} 个在售产品的预测",
                processedProductCount, activeProductCount);

        return forecasts;
    }

    /**
     * 根据销量历史对产品进行分类
     *
     * @param productWindows 产品窗口数据
     * @return 产品类别
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
     *
     * @param productWindows 产品窗口数据
     * @param category 产品类别
     * @return 增长率
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
} 