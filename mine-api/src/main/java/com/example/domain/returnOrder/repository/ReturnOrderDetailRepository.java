package com.example.domain.returnOrder.repository;

import com.example.domain.returnOrder.entity.ReturnOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 退货订单详情仓库
 */
@Repository
public interface ReturnOrderDetailRepository extends JpaRepository<ReturnOrderDetail, Integer> {
    
    /**
     * 根据退货订单ID查询退货订单详情
     *
     * @param returnOrderId 退货订单ID
     * @return 退货订单详情列表
     */
    List<ReturnOrderDetail> findByReturnOrderId(Integer returnOrderId);
    
    /**
     * 根据商品ID查询退货订单详情
     *
     * @param productId 商品ID
     * @return 退货订单详情列表
     */
    List<ReturnOrderDetail> findByProductId(Integer productId);
    
    /**
     * 根据订单ID查询退货订单详情统计
     *
     * @param returnOrderId 退货订单ID
     * @return 详情数量
     */
    @Query("SELECT COUNT(r) FROM ReturnOrderDetail r WHERE r.returnOrder.id = ?1")
    long countByReturnOrderId(Integer returnOrderId);
}