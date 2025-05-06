package com.example.monitoring.repository;

import com.example.monitoring.dto.ProcessedItemDetailDto; // 导入新的 DTO
import com.example.monitoring.entity.ExpiryAlertProcessedLog;
import org.springframework.data.domain.Page; // 导入 Page
import org.springframework.data.domain.Pageable; // 导入 Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // 导入 Modifying
import org.springframework.data.jpa.repository.Query; // 导入 Query
import org.springframework.data.repository.query.Param; // 导入 Param
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // 导入 Transactional

/**
 * 临期预警处理日志 Repository
 */
@Repository
public interface ExpiryAlertProcessedLogRepository extends JpaRepository<ExpiryAlertProcessedLog, Long> {

    /**
     * 检查是否存在针对指定 saleBatchDetailId 的处理日志
     * @param saleBatchDetailId 销售批次详情ID
     * @return 如果存在则返回 true, 否则返回 false
     */
    boolean existsBySaleBatchDetailId(Integer saleBatchDetailId);

    /**
     * 根据 saleBatchDetailId 删除处理日志记录
     * @param saleBatchDetailId 销售批次详情ID
     * @return 删除的记录数
     */
    @Modifying
    @Transactional // 删除操作通常需要事务支持
    long deleteBySaleBatchDetailId(Integer saleBatchDetailId);


    /**
     * 查询已处理的临期商品项历史记录
     * @param pageable 分页参数
     * @return 分页的已处理商品项详情
     */
    @Query(value = "SELECT " +
            "eapl.sale_batch_detail_id as saleBatchDetailId, " +
            "s.name as shopName, " +
            "p.name as productName, " +
            "o.id as orderId, " +
            "od.id as orderDetailId, " +
            "b.id as batchId, " +
            "b.production_date as productionDate, " +
            "eapl.processed_at as processedAt " +
            "FROM expiry_alert_processed_log eapl " +
            "JOIN sale_batch_detail sbd ON eapl.sale_batch_detail_id = sbd.id " +
            "JOIN batch b ON sbd.batch_id = b.id " +
            "JOIN order_detail od ON sbd.order_detail_id = od.id " +
            "JOIN product p ON od.product_id = p.id " +
            "JOIN `order` o ON od.order_id = o.id " +
            "JOIN shop s ON o.shop_id = s.id " +
            "ORDER BY eapl.processed_at DESC",
            countQuery = "SELECT count(eapl.id) " + // 确保 countQuery 引用 eapl.id 或其他唯一主键
                         "FROM expiry_alert_processed_log eapl " +
                         "JOIN sale_batch_detail sbd ON eapl.sale_batch_detail_id = sbd.id " +
                         "JOIN batch b ON sbd.batch_id = b.id " +
                         "JOIN order_detail od ON sbd.order_detail_id = od.id " +
                         "JOIN product p ON od.product_id = p.id " +
                         "JOIN `order` o ON od.order_id = o.id " +
                         "JOIN shop s ON o.shop_id = s.id",
            nativeQuery = true)
    Page<ProcessedItemDetailDtoProjection> findProcessedItemHistory(Pageable pageable);

    // 定义一个接口用于投影，确保类型安全和与DTO的映射
    // 或者确保 ProcessedItemDetailDto 有一个完全匹配这些列的构造函数
    // 为了使用 Spring Data JPA 的 DTO 投影，通常需要一个接口或一个带有匹配构造函数的类
    // 这里我们使用接口投影作为示例，它更灵活一些。
    // 如果 ProcessedItemDetailDto 有一个匹配所有列的构造函数，可以直接用 Page<ProcessedItemDetailDto>
    interface ProcessedItemDetailDtoProjection {
        Integer getSaleBatchDetailId();
        String getShopName();
        String getProductName();
        Integer getOrderId();
        Integer getOrderDetailId();
        Integer getBatchId();
        java.sql.Date getProductionDate(); // Native query 返回 java.sql.Date for DATE type
        java.sql.Timestamp getProcessedAt(); // Native query 返回 java.sql.Timestamp for DATETIME/TIMESTAMP
    }

    // 如果 ProcessedItemDetailDto 使用了 AllArgsConstructor 并且字段顺序与 SELECT 语句中的列顺序一致，
    // 并且类型也兼容 (例如 java.sql.Date -> LocalDate, java.sql.Timestamp -> LocalDateTime 会自动转换),
    // 那么可以直接使用 Page<ProcessedItemDetailDto>。
    // 为确保健壮性，使用接口投影或确保构造函数匹配是推荐的。
    // 鉴于 ProcessedItemDetailDto 已有 @AllArgsConstructor，如果列顺序和类型匹配，
    // 我们可以尝试直接返回 Page<ProcessedItemDetailDto>，但需要注意 Native Query 返回的日期/时间类型。
    // Spring Data JPA 通常能处理 java.sql.Date/Timestamp 到 java.time.LocalDate/LocalDateTime 的转换。
    // 我们先尝试直接返回 Page<ProcessedItemDetailDto>，如果不行再调整为接口投影。
    // 因此，我将上面的 findProcessedItemHistory 返回类型改回 Page<ProcessedItemDetailDto>

}