package com.example.domain.order.repository;

import com.example.domain.order.entity.SaleBatchDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // 导入 Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param; // 导入 Param
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 销售批次详情数据访问接口
 */
@Repository
public interface SaleBatchDetailRepository extends JpaRepository<SaleBatchDetail, Integer>, QuerydslPredicateExecutor<SaleBatchDetail> {
    
    /**
     * 根据订单详情ID查询销售批次详情列表
     *
     * @param orderDetailId 订单详情ID
     * @return 销售批次详情列表
     */
    List<SaleBatchDetail> findByOrderDetailId(Integer orderDetailId);

    /**
     * 根据批次ID查询销售批次详情列表
     *
     * @param batchId 批次ID
     * @return 销售批次详情列表
     */
    List<SaleBatchDetail> findByBatchId(Integer batchId);

    /**
     * 根据订单ID列表查询销售批次详情，并急切加载关联的 Batch, OrderDetail, 和 Product
     * @param orderIds 订单ID列表
     * @return 包含完整关联信息的销售批次详情列表
     */
    @Query("SELECT sbd FROM SaleBatchDetail sbd " +
           "JOIN FETCH sbd.batch b " +
           "JOIN FETCH sbd.orderDetail od " +
           "JOIN FETCH od.product p " +
           "WHERE od.order.id IN :orderIds")
    List<SaleBatchDetail> findWithDetailsByOrderIds(@Param("orderIds") List<Integer> orderIds);
}