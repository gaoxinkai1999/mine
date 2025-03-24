package com.example.domain.forecast.service;

import com.example.HoltWintersForecast;
import com.example.domain.product.service.ProductService;
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.domain.statistics.service.StatisticsService;
import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ForecastService {

    @Autowired
    private ProductService productService;
    @Autowired
    private StatisticsService statisticsService;


    /**
     * 根据商品id获取商品的销量数据
     *
     * @param data
     * @param productId
     * @return
     */

    public double[] getProductData(Map<LocalDate, SalesStatisticsDTO> data, int productId) {
        // 获取商品的最早销售日期
        LocalDate earliestSaleDate = productService.getEarliestSaleDateByProductId(productId);
        // 提前商品销售以来的每日销售数据
        List<Map.Entry<LocalDate, SalesStatisticsDTO>> list = data.entrySet()
                                                                  .stream()
                                                                  .filter(entry -> entry.getKey()
                                                                                        .isAfter(earliestSaleDate))
                                                                  .toList();
        // 从每日销售数据中提取该商品id的销量数组
        double[] array = list.stream()
                             .sorted(Map.Entry.comparingByKey())
                             .map(entry -> {
                                 double v = entry.getValue()
                                                 .getProductSalesInfoDTOS()
                                                 .stream()
                                                 .filter(productSalesInfoDTO -> productSalesInfoDTO.getProductId() == productId)
                                                 .map(ProductSalesInfoDTO::getQuantity)
                                                 .mapToDouble(Integer::intValue)
                                                 .findFirst()
                                                 .orElse(0.0);
                                 return v;

                             })
                             .mapToDouble(Double::doubleValue)
                             .toArray();
        return array;

    }

    /**
     * 提取总体销量信息数组
     */
    public double[] getOverallSalesData(Map<LocalDate, SalesStatisticsDTO> data) {
        return data.entrySet()
                   .stream()
                   .sorted(Map.Entry.comparingByKey())
                   .mapToDouble(entry -> {
                       double v = entry.getValue()
                                       .getTotalProfit()
                                       .doubleValue();
                       return v;

                   })
                   .toArray();
    }

    public double forecast(double[] data, int forecastDays) throws MyException {
        // 预测所需数据量
        if (data.length <= forecastDays * 5 ) {
            throw new MyException("数据量不足");
        }
        double[] shortTermData = Arrays.copyOfRange(data, data.length - forecastDays * 5, data.length);
        double[] doubles = HoltWintersForecast.dailyForecast(shortTermData, forecastDays);
        log.info("预测数据：{}", shortTermData);
        log.info("预测结果：{}", doubles);
        return Arrays.stream(doubles)
                     .sum();
    }

    // /double数组日数据聚合为周数据,倒着聚合，最前面不足7天的数据不聚合
    public double[] aggregateToWeeklyData(double[] data) {
        int length = data.length;
        int offset = length % 7; // 计算前面不足7天的部分
        int week = length / 7;   // 完整周数

        double[] weeklyData = new double[week];

        for (int i = 0; i < week; i++) {
            int start = offset + i * 7;      // 跳过前面不足7天的部分
            int end = start + 7;             // 每周7天
            weeklyData[i] = Arrays.stream(data, start, end)
                                  .sum();
        }
        return weeklyData;
    }


}
