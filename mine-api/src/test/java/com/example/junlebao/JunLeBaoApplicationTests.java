package com.example.junlebao;


import com.example.domain.forecast.service.ForecastService;
import com.example.domain.product.service.ProductService;
import com.example.domain.statistics.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

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

    @Test
    void contextLoads() {

    }

    @Test
    void a() {



    }

    /**
     * 测试将商品每日销售数据导出为CSV文件
     * 这个测试方法会获取指定商品ID的每日销售数据，并将其导出为Prophet格式的CSV文件
     */
    @Test
    void testExportDailySalesToCsv() throws IOException {

    }
}



