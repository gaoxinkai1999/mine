package com.example.domain.forecast.model;

/**
 * 商品销量分类枚举
 * 用于对商品进行销量级别分类，便于进行预测
 */
public enum ProductCategory {
    /**
     * 高销量商品
     */
    HIGH_VOLUME,
    
    /**
     * 中等销量商品
     */
    MEDIUM_VOLUME,
    
    /**
     * 低销量商品
     */
    LOW_VOLUME
} 