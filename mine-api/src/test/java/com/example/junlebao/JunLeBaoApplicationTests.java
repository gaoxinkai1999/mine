package com.example.junlebao;


import com.alibaba.fastjson2.JSONObject;
import com.example.SalesForecastServicePlus;
import com.example.domain.forecast.model.ForecastResult;
import com.example.domain.forecast.service.SalesForecastService;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.domain.statistics.service.StatisticsService;
import com.example.utils.CsvExportUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
//@Transactional
class JunLeBaoApplicationTests {
    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    SalesForecastService salesForecastService;
    @Autowired
    SalesForecastServicePlus salesForecastServicePlus;

    @Test
    void contextLoads() {
        Long start=System.currentTimeMillis();

        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 4);
        Map<LocalDate, SalesStatisticsDTO> localDateSalesStatisticsDTOMap = statisticsService.calculateDailyStatistics(startDate, endDate);
        SalesForecastServicePlus.ForecastResult forecastResult = salesForecastServicePlus.generateForecast(localDateSalesStatisticsDTOMap);
        val jsonString = JSONObject.toJSONString(forecastResult);
        System.out.println(jsonString);
        System.out.println("耗时："+(System.currentTimeMillis()-start));
    }
    @Test
    void a() {
        Long start=System.currentTimeMillis();

        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 4);
        Map<LocalDate, SalesStatisticsDTO> localDateSalesStatisticsDTOMap = statisticsService.calculateDailyStatistics(startDate, endDate);
        ForecastResult forecastResult = salesForecastService.generateForecast(localDateSalesStatisticsDTOMap);
        val jsonString = JSONObject.toJSONString(forecastResult);
        System.out.println(jsonString);
        System.out.println("耗时："+(System.currentTimeMillis()-start));
    }

    /**
     * 测试将商品每日销售数据导出为CSV文件
     * 这个测试方法会获取指定商品ID的每日销售数据，并将其导出为Prophet格式的CSV文件
     */
    @Test
    void testExportDailySalesToCsv() throws IOException {
        // 测试使用的商品ID（请根据实际情况调整）
        int productId = 87;
        
        // 获取每日销售数据
        List<Map<String, Object>> dailySalesData = statisticsService.getDailySalesByProductId(productId);
        
        if (dailySalesData == null || dailySalesData.isEmpty()) {
            log.info("商品 ID {} 没有销售数据", productId);
            return;
        }
        
        log.info("获取到商品 ID {} 的销售数据 {} 条", productId, dailySalesData.size());
        
        // 创建输出目录
        String outputDir = "export";
        File directory = new File(outputDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        // 生成带有时间戳的文件名
        String fileName = String.format("%s/product_%d_prophet_data_%s.csv", 
                outputDir, 
                productId, 
                LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        
        File csvFile = new File(fileName);
        
        // 使用CsvExportUtils的Prophet格式导出方法
        CsvExportUtils.exportProphetData(dailySalesData, csvFile);
        
        log.info("Prophet格式CSV文件已成功导出到: {}", csvFile.getAbsolutePath());
    }
}



