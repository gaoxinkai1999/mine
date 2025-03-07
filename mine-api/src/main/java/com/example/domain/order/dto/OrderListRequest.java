package com.example.domain.order.dto;

import lombok.Data;

import java.time.LocalDate;
@Data
public class OrderListRequest {

    private Integer shopId;
    private LocalDate startDate;
    private LocalDate endDate;


    private Integer page = 0;


    private Integer size = 10;
}
