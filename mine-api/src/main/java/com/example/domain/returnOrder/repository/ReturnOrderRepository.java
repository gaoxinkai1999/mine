package com.example.domain.returnOrder.repository;

import com.example.domain.returnOrder.entity.ReturnOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 退货订单仓库
 */
@Repository
public interface ReturnOrderRepository extends JpaRepository<ReturnOrder, Integer> {
    
    /**
     * 根据商店ID查询退货订单
     *
     * @param shopId 商店ID
     * @return 退货订单列表
     */
    List<ReturnOrder> findByShopId(Integer shopId);
    
    /**
     * 根据时间范围查询退货订单
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 退货订单列表
     */
    @Query("SELECT r FROM ReturnOrder r WHERE DATE(r.createTime) BETWEEN ?1 AND ?2")
    List<ReturnOrder> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据商店ID和时间范围查询退货订单
     *
     * @param shopId    商店ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 退货订单列表
     */
    @Query("SELECT r FROM ReturnOrder r WHERE r.shop.id = ?1 AND DATE(r.createTime) BETWEEN ?2 AND ?3")
    List<ReturnOrder> findByShopIdAndDateBetween(Integer shopId, LocalDate startDate, LocalDate endDate);
} 