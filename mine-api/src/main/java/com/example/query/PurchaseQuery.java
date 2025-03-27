package com.example.query;

import com.example.interfaces.BaseQuery;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 采购单查询条件
 */
@Getter
@Setter
@Builder
public class PurchaseQuery implements BaseQuery {
    /**
     * 采购单ID
     */
    private Integer id;

    /**
     * 采购单ID列表
     */
    private Integer[] ids;
    
    /**
     * 采购单状态
     */
    private String state;
    
    /**
     * 创建时间开始
     */
    private LocalDateTime createTimeStart;
    
    /**
     * 创建时间结束
     */
    private LocalDateTime createTimeEnd;
    

    /**
     * 包含关联数据
     */
    @Builder.Default
    private Set<Include> includes = Set.of();
    
    /**
     * 关联数据枚举
     */
    public enum Include {
        /**
         * 包含采购明细
         */
        PURCHASE_DETAILS,
        
        /**
         * 包含商品
         */
        PRODUCT
    }
} 