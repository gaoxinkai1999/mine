package com.example.domain.batch.repository;

import com.example.domain.batch.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface BatchRepository extends JpaRepository<Batch, Integer> {
    /**
     * 根据批次号查询批次信息。
     *
     * @param batchNumber 批次号
     * @return 包含批次信息的 Optional 对象，如果未找到则返回空 Optional
     */
    Optional<Batch> findByBatchNumber(String batchNumber);

    /**
     * 根据产品ID和状态查询批次列表。
     *
     * @param productId 产品ID
     * @param status 状态（true 或 false）
     * @return 符合条件的批次列表
     */
    List<Batch> findByProductIdAndStatus(Integer productId, Boolean status);

    /**
     * 查询有效批次列表。
     * 查询条件包括：产品ID、过期日期大于等于指定日期且状态为 true 的批次。
     *
     * @param productId 产品ID
     * @param date 指定的日期
     * @return 符合条件的有效批次列表
     */
    @Query("SELECT b FROM Batch b WHERE b.product.id = :productId AND b.expirationDate >= :date AND b.status = true")
    List<Batch> findValidBatches(@Param("productId") Integer productId, @Param("date") LocalDate date);

    /**
     * 根据批次号前缀统计符合条件的批次数量。
     *
     * @param prefix 批次号前缀
     * @return 符合条件的批次数量
     */
    @Query("SELECT COUNT(b) FROM Batch b WHERE b.batchNumber LIKE CONCAT(:prefix, '%')")
    int countByBatchNumberPrefix(@Param("prefix") String prefix);

    /**
     * 根据商品ID查询库存中存在的批次列表 (仅查询批次信息)
     * @param productId 商品ID
     * @return 批次列表
     */
    // 注意：这个 JPQL 查询假设 Inventory 实体与 Batch 有关联，并且 quantity > 0
    // 如果 Inventory 和 Batch 没有直接关联，需要修改查询逻辑，可能需要连接 Inventory 表
    @Query("SELECT DISTINCT b FROM Batch b JOIN Inventory i ON b.id = i.batch.id WHERE i.product.id = :productId AND i.quantity > 0")
    List<Batch> findBatchesInStockByProductId(@Param("productId") Integer productId);
}
