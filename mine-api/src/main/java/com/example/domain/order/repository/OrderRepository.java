package com.example.domain.order.repository;

import com.example.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List; // 导入 List
import java.util.Optional;
import com.example.monitoring.dto.NearExpiryBatchInfo; // 导入临期 DTO

/**
 * Order 数据访问仓库
 */
public interface OrderRepository extends JpaRepository<Order, Integer>  {

    /**
     * 查询指定商家和商品的最新订单创建时间
     * @param shopId 商家ID
     * @param productId 商品ID
     * @return 最新的订单创建时间 (LocalDateTime), 如果没有则返回 Optional.empty()
     */
    @Query("SELECT MAX(o.createTime) FROM Order o JOIN o.orderDetails od WHERE o.shop.id = :shopId AND od.product.id = :productId")
    Optional<LocalDateTime> findLatestOrderCreateTimeByShopAndProduct(@Param("shopId") Integer shopId, @Param("productId") Integer productId);


    /**
     * 使用原生 SQL 查询查找临期批次信息
     * @return 临期批次信息 DTO 列表
     */
    @Query(value = """
        SELECT
            s.id AS shopId,                 -- 别名匹配 DTO getter: getShopId()
            s.name AS shopName,             -- 别名匹配 DTO getter: getShopName()
            o.id AS orderId,                -- 别名匹配 DTO getter: getOrderId()
            od.id AS orderDetailId,         -- 新增：别名匹配 DTO getter: getOrderDetailId()
            sbd.id AS saleBatchDetailId,    -- 新增：别名匹配 DTO getter: getSaleBatchDetailId()
            o.create_time AS latestOrderTime, -- 别名匹配 DTO getter: getLatestOrderTime()
            p.name AS productName,          -- 别名匹配 DTO getter: getProductName()
            od.quantity AS productQuantity, -- 别名匹配 DTO getter: getProductQuantity()
            b.production_date AS productionDate, -- 别名匹配 DTO getter: getProductionDate()
            p.expiry_monitoring_threshold_days AS expiryMonitoringThresholdDays, -- 别名匹配 DTO getter: getExpiryMonitoringThresholdDays()
            DATEDIFF(CURDATE(), b.production_date) AS daysSinceProduction, -- 别名匹配 DTO getter: getDaysSinceProduction()
            p.id AS productId,              -- 额外添加，可能有用
            b.id AS batchId                 -- 额外添加，可能有用
        FROM shop s
                 INNER JOIN `order` o ON s.id = o.shop_id
                 INNER JOIN order_detail od ON o.id = od.order_id
                 INNER JOIN product p ON od.product_id = p.id
                 INNER JOIN sale_batch_detail sbd ON od.id = sbd.order_detail_id
                 INNER JOIN batch b ON sbd.batch_id = b.id
                 LEFT JOIN expiry_alert_processed_log eapl ON sbd.id = eapl.sale_batch_detail_id -- 左连接到处理日志表
       WHERE s.is_del = 0  -- 只查询未删除的商家
          AND p.is_batch_managed = 1  -- 只查询批次管理商品
          AND b.production_date IS NOT NULL  -- 确保生产日期不为空
          AND DATEDIFF(CURDATE(), b.production_date) >= p.expiry_monitoring_threshold_days  -- 距今天数大于等于阈值
          AND eapl.id IS NULL -- 只选择那些在处理日志表中没有对应记录的（即未被处理的）
          AND o.create_time = (
            SELECT MAX(o2.create_time)
            FROM `order` o2
                     INNER JOIN order_detail od2 ON o2.id = od2.order_id
                     INNER JOIN product p2 ON od2.product_id = p2.id
                     INNER JOIN sale_batch_detail sbd2 ON od2.id = sbd2.order_detail_id
                     INNER JOIN batch b2 ON sbd2.batch_id = b2.id
            WHERE o2.shop_id = s.id
              AND p2.id = p.id  -- 针对同一商品
              AND p2.is_batch_managed = 1
              AND b2.production_date IS NOT NULL
              AND DATEDIFF(CURDATE(), b2.production_date) >= p2.expiry_monitoring_threshold_days -- 同上，改为 >=
        )
        ORDER BY s.id, p.name, o.create_time DESC;
        """, nativeQuery = true)
    List<NearExpiryBatchInfo> findNearExpiryBatchInfoNative();

}
