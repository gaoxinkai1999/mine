package com.example.domain.statistics.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class OverallTrendRequest {
    // Getter 和 Setter 方法
    private int period;    // 移动平均周期（天数）
    private LocalDate startDate; // 开始日期
    private LocalDate endDate;   // 结束日期

}