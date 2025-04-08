package com.example.junlebao;


import com.example.domain.forecast.service.ForecastService;
import com.example.domain.product.dto.ProductDto;
import com.example.domain.product.service.ProductService;
import com.example.domain.purchase.service.PurchaseService;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.domain.statistics.service.StatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@SpringBootTest
//@Transactional
class JunLeBaoApplicationTests {
    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    ProductService productService;
    @Autowired
    ForecastService forecastService;
    @Autowired
    PurchaseService purchaseService;

    @Test
    void contextLoads() {
        List<ProductDto> products = productService.getProducts();

        // 取出商品id
        int[] array = products.stream()
                              .mapToInt(ProductDto::getId)
                              .toArray();
        Map<Integer, Map<String, Integer>> integerMapMap = purchaseService.calculatePurchaseQuantity(array);
        System.out.println(integerMapMap);
    }

    @Test
    void a() {
        BigDecimal totalBudget = new BigDecimal("10000");
        int[] a = {87,88,99,107};
        List<PurchaseService.ProductBudgetInfo> list = productService.getProducts()
                                                                     .stream().filter(productDto -> Arrays.stream(a).anyMatch(x -> x == productDto.getId()))
                                                                     .map(productDto -> {
                                                                         PurchaseService.ProductBudgetInfo productBudgetInfo = new PurchaseService.ProductBudgetInfo(productDto.getId(), productDto.getCostPrice(), productDto.getProductStockDTO()
                                                                                                                                                                                                                              .getTotalInventory(), productDto.getName());
                                                                         return productBudgetInfo;
                                                                     })
                                                                     .toList();

        Map<Integer, Integer> integerIntegerMap = purchaseService.allocateBudgetForProducts(totalBudget, list);
        System.out.println(integerIntegerMap);

        BigDecimal totalCost = BigDecimal.ZERO;
        for (PurchaseService.ProductBudgetInfo info : list) {
            int quantity = integerIntegerMap.getOrDefault(info.getProductId(), 0);
            BigDecimal cost = info.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
            System.out.println("商品ID: " + info.getProductId() + ", 名称: " + info.getProductName() + ", 采购数量: " + quantity + ", 采购金额: " + cost + ", 采购后库存: " + (info.getCurrentStock() + quantity));
            totalCost = totalCost.add(cost);
        }
        System.out.println("总消耗金额: " + totalCost);
    }

    /**
     * 测试将商品每日销售数据导出为CSV文件
     * 这个测试方法会获取指定商品ID的每日销售数据，并将其导出为Prophet格式的CSV文件
     */
    @Test
    void b() throws IOException {
        Map<YearMonth, SalesStatisticsDTO> yearMonthSalesStatisticsDTOMap = statisticsService.calculateMonthlyStatisticsOptimized(LocalDate.of(2024, 2, 1), LocalDate.of(2025, 4, 7));

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(System.out, yearMonthSalesStatisticsDTOMap);



    }

    @Test
    void testWeeklyAggregateProductSalesData() {
        int targetProductId = 87;  // 指定商品ID，替换为实际ID
        LocalDate startDate = LocalDate.of(2024, 4, 1);
        LocalDate endDate = LocalDate.of(2025, 4, 3);


        Map<LocalDate, com.example.domain.statistics.dto.response.SalesStatisticsDTO> dailyStats =
                statisticsService.calculateDailyStatistics(startDate, endDate);

        // 提取每日销量数据
        List<Map<String, Object>> dailyData = new ArrayList<>();
        for (Map.Entry<LocalDate, com.example.domain.statistics.dto.response.SalesStatisticsDTO> entry : dailyStats.entrySet()) {
            LocalDate date = entry.getKey();
            List<com.example.domain.statistics.dto.response.ProductSalesInfoDTO> productList = entry.getValue()
                                                                                                    .getProductSalesInfoDTOS();

            double salesQuantity = 0.0;
            for (com.example.domain.statistics.dto.response.ProductSalesInfoDTO product : productList) {
                if (product.getProductId() == targetProductId) {
                    salesQuantity = product.getQuantity();
                    break;
                }
            }

            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("ds", date.toString());
            dataPoint.put("y", salesQuantity);
            dailyData.add(dataPoint);
        }

        // 按日期排序
        dailyData.sort(Comparator.comparing(m -> m.get("ds")
                                                  .toString()));

        // 舍弃最早不足7天的数据，确保最近的周数据完整
        int daysToRemove = dailyData.size() % 7;
        List<Map<String, Object>> trimmedData = dailyData.subList(daysToRemove, dailyData.size());

        // 进行每7天一组的聚合
        List<Map<String, Object>> weeklyData = new ArrayList<>();
        for (int i = 0; i < trimmedData.size(); i += 7) {
            double sum = 0.0;
            String weekEndDate = "";
            for (int j = 0; j < 7; j++) {
                Map<String, Object> dayData = trimmedData.get(i + j);
                sum += ((Number) dayData.get("y")).doubleValue();
                weekEndDate = dayData.get("ds")
                                     .toString();
            }
            Map<String, Object> weekPoint = new HashMap<>();
            weekPoint.put("ds", weekEndDate);
            weekPoint.put("y", sum);
            weeklyData.add(weekPoint);
        }

        // 打印周聚合后的结果
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(Map.of("weekly_data", weeklyData));
            System.out.println(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testExtractProductDailySalesData() {
    }
}



