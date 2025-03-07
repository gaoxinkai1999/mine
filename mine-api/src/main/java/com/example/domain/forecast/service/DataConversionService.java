package com.example.domain.forecast.service;

import com.example.domain.forecast.model.DailySales;
import com.example.domain.forecast.model.ForecastResult;
import com.example.domain.forecast.model.ProductDailySales;
import com.example.domain.forecast.model.ProductSales;
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据转换服务
 * 负责将原始销售数据转换为预测所需的格式
 */
@Service
@Slf4j
public class DataConversionService {

    private static final int SCALE = 2; // 小数位数

    /**
     * 将销售统计数据转换为日销售数据列表
     *
     * @param salesDataMap 销售统计数据映射
     * @return 日销售数据列表
     */
    public List<DailySales> convertToDailySales(Map<LocalDate, SalesStatisticsDTO> salesDataMap) {
        log.info("开始转换销售数据为日销售格式，数据天数: {}", salesDataMap.size());
        
        List<DailySales> dailySalesList = new ArrayList<>();
        
        for (Map.Entry<LocalDate, SalesStatisticsDTO> entry : salesDataMap.entrySet()) {
            LocalDate date = entry.getKey();
            SalesStatisticsDTO statsDTO = entry.getValue();
            
            DailySales dailySales = new DailySales();
            dailySales.setDate(date);
            
            double totalSales = statsDTO.getTotalSales().doubleValue();
            double totalProfit = statsDTO.getTotalProfit().doubleValue();
            
            dailySales.setSales(totalSales);
            dailySales.setProfit(totalProfit);
            
            dailySalesList.add(dailySales);
        }
        
        log.info("销售数据转换完成，共处理{}天数据", dailySalesList.size());
        return dailySalesList;
    }

    /**
     * 将销售统计数据转换为产品销售数据列表
     *
     * @param salesDataMap 销售统计数据映射
     * @return 产品销售数据列表
     */
    public List<ProductSales> convertToProductSales(Map<LocalDate, SalesStatisticsDTO> salesDataMap) {
        log.info("开始转换销售数据为产品销售格式，数据天数: {}", salesDataMap.size());
        
        Map<Integer, ProductSales> productSalesMap = new HashMap<>();
        
        for (Map.Entry<LocalDate, SalesStatisticsDTO> entry : salesDataMap.entrySet()) {
            LocalDate date = entry.getKey();
            SalesStatisticsDTO statsDTO = entry.getValue();
            
            if (statsDTO.getProductSalesInfoDTOS() != null) {
                for (ProductSalesInfoDTO productSaleDTO : statsDTO.getProductSalesInfoDTOS()) {
                    if (productSaleDTO.getQuantity() > 0) {
                        int productId = productSaleDTO.getProductId();
                        String productName = productSaleDTO.getProductName();
                        int quantity = productSaleDTO.getQuantity();
                        double sales = productSaleDTO.getTotalSales().doubleValue();
                        double profit = productSaleDTO.getTotalProfit().doubleValue();
                        
                        // 检查产品是否已存在于Map中
                        if (!productSalesMap.containsKey(productId)) {
                            // 创建新的ProductSales对象
                            ProductSales ps = new ProductSales();
                            ps.setProductId(productId);
                            ps.setProductName(productName);
                            productSalesMap.put(productId, ps);
                        }
                        
                        // 为该产品添加当天的销售数据
                        ProductSales productSales = productSalesMap.get(productId);
                        productSales.getDailySales().put(date, new ProductDailySales(quantity, sales, profit));
                    }
                }
            }
        }
        
        List<ProductSales> productSalesList = new ArrayList<>(productSalesMap.values());
        log.info("销售数据转换完成，共处理{}个产品", productSalesList.size());
        return productSalesList;
    }

    /**
     * 创建空的预测结果对象
     *
     * @return 预测结果对象
     */
    public ForecastResult createEmptyForecast() {
        log.info("创建空的预测结果对象");
        
        ForecastResult result = new ForecastResult();
        result.setMethodResults(new HashMap<>());
        result.setOverallSalesForecast(BigDecimal.ZERO);
        result.setOverallProfitForecast(BigDecimal.ZERO);
        result.setLowerBound(BigDecimal.ZERO);
        result.setUpperBound(BigDecimal.ZERO);
        result.setProductForecasts(new HashMap<>());
        result.setHistoricalTrendDirection("无趋势");
        result.setForecastTrendDirection("持平");
        result.setTrendReliability(BigDecimal.valueOf(0.3));
        result.setForecastAccuracy(BigDecimal.valueOf(0.3));
        result.setAccuracyLevel("参考性有限");
        result.setForecastVariability(BigDecimal.valueOf(0.3));
        result.setDataQualityScore(BigDecimal.valueOf(0.3));
        result.setReliabilityScore(BigDecimal.valueOf(0.3));
        result.setConfidenceScore(BigDecimal.valueOf(0.3));
        result.setAverageProfitRate(BigDecimal.valueOf(0.25));
        result.setForecastDate(LocalDate.now());
        result.setForecastPeriodDays(14);
        result.setMessage("数据不足，无法生成可靠预测");
        
        return result;
    }
} 