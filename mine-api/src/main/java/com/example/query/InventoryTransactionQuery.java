package com.example.query;

import com.example.domain.inventory.dto.OperationType;
import com.example.interfaces.BaseQuery;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 库存变动记录查询条件
 */
@Data
@Builder
public class InventoryTransactionQuery implements BaseQuery {
    /**
     * 商品ID
     */
    private Integer productId;

    /**
     * 批次ID
     */
    private Integer batchId;

    /**
     * 订单ID
     */
    private Integer orderId; // 销售订单ID

    /**
     * 采购订单ID
     */
    private Integer purchaseId;

    /**
     * 退货订单ID
     */
    private Integer returnOrderId;

    /**
     * 操作类型
     */
    private OperationType operationType;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 需要包含的关联数据
     */
    @Builder.Default
    private Set<Include> includes = Set.of();

    /**
     * 可包含的关联数据枚举
     */
    public enum Include {
        /**
         * 包含商品信息
         */
        PRODUCT,

        /**
         * 包含批次信息
         */
        BATCH,

        /**
         * 包含订单信息
         */
        ORDER, // 销售订单

        /**
         * 包含采购订单信息
         */
        PURCHASE,

        /**
         * 包含退货订单信息
         */
        RETURN_ORDER
    }
}