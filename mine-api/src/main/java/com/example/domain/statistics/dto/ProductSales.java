package com.example.domain.statistics.dto;

import java.time.LocalDate;

/**
 * 产品销售数据模型
 * 用于存储每个产品每天的销售数据
 * 这是预测服务内部使用的类，用于处理从ProductSalesInfoDTO转换来的数据
 */

public class ProductSales {
    private int productId;          // 产品ID
    private String productName;     // 产品名称
    private LocalDate date;         // 销售日期
    private int quantity;           // 销售数量
    private double sales;           // 销售额
    private double profit;          // 利润

    public ProductSales() {
    }

    public ProductSales(int productId, String productName, LocalDate date, int quantity, double sales, double profit) {
        this.productId = productId;
        this.productName = productName;
        this.date = date;
        this.quantity = quantity;
        this.sales = sales;
        this.profit = profit;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
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

    @Override
    public String toString() {
        return "ProductSales{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", date=" + date +
                ", quantity=" + quantity +
                ", sales=" + sales +
                ", profit=" + profit +
                '}';
    }
}