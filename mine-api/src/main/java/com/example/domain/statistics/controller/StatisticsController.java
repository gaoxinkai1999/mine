package com.example.domain.statistics.controller;

import com.example.domain.statistics.dto.request.MovingAverageLineRequest;
import com.example.domain.statistics.dto.response.MovingAverageLineDTO;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.domain.statistics.dto.response.ShopStatisticsDTO;
import com.example.domain.statistics.service.StatisticsService;
import com.example.exception.MyException;
import com.example.utils.DataExtractor;
import com.example.utils.ProfitExtractor;
import com.example.utils.QuantityExtractor;
import com.example.utils.SalesAmountExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Tag(name = "statistics", description = "统计数据接口")
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @Operation(summary = "获取商家统计数据")
    @GetMapping("/shop")
    public List<ShopStatisticsDTO> getShopStatistics() {
        return statisticsService.calculateShopStatistics();
    }

    @Operation(summary = "获取日期范围统计数据")
    @GetMapping("/date-range")
    public SalesStatisticsDTO getDateRangeStatistics(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return statisticsService.calculateDateRangeStatistics(startDate, endDate);
    }
    @Operation(summary = "获取月度统计数据")
    @GetMapping("/monthly")
    public Map<YearMonth, SalesStatisticsDTO> getMonthlyStatistics(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ){
        return statisticsService.calculateMonthlyStatisticsOptimized(startDate, endDate);
    }

    @Operation(summary = "获取每日统计数据")
    @GetMapping("/daily")
    public Map<LocalDate, SalesStatisticsDTO> getDailyStatistics(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return statisticsService.calculateDailyStatistics(startDate, endDate);
    }

    @Operation(summary = "获取产品移动平均线数据")
    @PostMapping("/products")
    public MovingAverageLineDTO getMovingAverage(
            @RequestBody MovingAverageLineRequest request,
            @RequestParam LocalDate startDate, // 添加 startDate 参数
            @RequestParam LocalDate endDate    // 添加 endDate 参数
    ) {
        if (request.getProductIds() == null || request.getProductIds().length == 0) {
            throw new MyException("产品ID数组不能为空");
        }
        // 移除 DataExtractor 创建逻辑，现在由 Service 处理
        // 直接调用更新后的 Service 方法
        return statisticsService.getMovingAverage(request, startDate, endDate);
    }

    @Operation(summary = "获取总体趋势数据")
    @GetMapping("/overall-trend")
    public MovingAverageLineDTO getOverallTrend(
            @RequestParam Integer period,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return statisticsService.calculateOverallTrend(
                period,
                startDate,
                endDate
        );
    }


}
