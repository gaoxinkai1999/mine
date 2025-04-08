package com.example.domain.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data

public class ProductWithPurchaseInfoDto {
    // 品类id
    private Integer categoryId;
    // 商品id
    private Integer id;
    // 商品名称
    private String name;
    // 采购单价
    private BigDecimal purchasePrice;
    // 当前总库存
    private Integer currentStock;
    // 预警数量
    private Integer warningQuantity;
    // 推荐采购数量
    private Integer recommendPurchaseQuantity;

    // 是否正常预测
    private Boolean forecastNormal = true;


}
