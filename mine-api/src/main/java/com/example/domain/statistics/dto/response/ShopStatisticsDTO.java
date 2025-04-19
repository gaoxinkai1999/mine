package com.example.domain.statistics.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 用于表示每个商家的统计结果。
 */
@Getter
@Setter
public class ShopStatisticsDTO {

    private int shopId; // 商家ID
    private String shopName; // 商家名称
    private String location; // 商家地址
    private BigDecimal arrears; // 商家欠款
    private BigDecimal totalSales; // 总销售额
    private BigDecimal totalProfit; // 总利润
    private BigDecimal averageMonthlyProfit; // 平均月利润
    private java.util.List<ProductSalesInfoDTO> productMonthlySalesList; // 每个商家下所有商品的平均月销售数据
}
