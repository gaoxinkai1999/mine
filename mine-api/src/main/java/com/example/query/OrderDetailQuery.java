package com.example.query;

import com.example.interfaces.BaseQuery;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 订单详情查询参数
 */
@Getter
@Builder
public class OrderDetailQuery implements BaseQuery {
    // 订单详情ID
    private Integer id;
    
    // 产品ID，用于查询特定产品的所有订单
    private Integer productId;
    
    // 订单ID
    private Integer orderId;
    
    // 时间范围
    private LocalDate startTime;
    private LocalDate endTime;
    
    // 关联加载选项
    @Builder.Default
    private Set<String> includes = new HashSet<>();

    // 预定义关联选项
    public static class Include {
        public static final String ORDER = "order";
        public static final String PRODUCT = "product";
        public static final String SHOP = "order.shop";
        
        // 常用组合
        public static Set<String> BASIC = Collections.emptySet();
        public static Set<String> WITH_ORDER = Set.of(ORDER);
        public static Set<String> WITH_PRODUCT = Set.of(PRODUCT);
        public static Set<String> WITH_ORDER_AND_SHOP = Set.of(ORDER, SHOP);
        public static Set<String> FULL = Set.of(ORDER, PRODUCT, SHOP);
    }
} 