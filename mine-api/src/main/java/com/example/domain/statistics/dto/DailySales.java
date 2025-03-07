package com.example.domain.statistics.dto;

import java.time.LocalDate;

/**
 * 日销售数据模型
 * 用于存储每天的销售总额和利润
 * 这是预测服务内部使用的类，用于处理从SalesStatisticsDTO转换来的数据
 */
public class DailySales {
    private LocalDate date;         // 销售日期
    private double sales;           // 销售总额
    private double profit;          // 利润
    private int orderCount;         // 订单数量

    public DailySales() {
    }

    public DailySales(LocalDate date, double sales, double profit, int orderCount) {
        this.date = date;
        this.sales = sales;
        this.profit = profit;
        this.orderCount = orderCount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getSales() {
        return sales;
    }

    public void setSales(double sales) {
        this.sales = sales;
    }

    public double getProfit() {
        return profit;
    }

    public void setProfit(double profit) {
        this.profit = profit;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    @Override
    public String toString() {
        return "DailySales{" +
                "date=" + date +
                ", sales=" + sales +
                ", profit=" + profit +
                ", orderCount=" + orderCount +
                '}';
    }
}

