package com.example.domain.statistics.dto.request;

import lombok.Data;

@Data
public class MovingAverageLineRequest {
    private int[] productIds;
    private String extractorType;
    private int period;
    private TaskType taskType;

    public enum TaskType {
        Profit,
        Quantity,
        SalesAmount;
    }
}
