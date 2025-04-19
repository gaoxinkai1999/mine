package com.example.domain.inventory.service;

import com.example.domain.batch.entity.Batch;
import com.example.domain.batch.entity.QBatch;
import com.example.domain.inventory.dto.OperationType;
import com.example.domain.inventory.entity.InventoryTransaction;
import com.example.domain.inventory.entity.QInventoryTransaction;
import com.example.domain.inventory.repository.InventoryTransactionRepository;
import com.example.domain.order.entity.Order;
import com.example.domain.order.entity.QOrder;
import com.example.domain.product.entity.Product;
import com.example.domain.product.entity.QProduct;
import com.example.domain.purchase.entity.Purchase;
import com.example.domain.purchase.entity.QPurchase;
import com.example.domain.returnOrder.entity.QReturnOrder;
import com.example.domain.returnOrder.entity.ReturnOrder;
import com.example.interfaces.BaseRepository;
import com.example.query.InventoryTransactionQuery;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存变动记录服务
 * 处理所有库存变动的记录、查询和统计
 */
@Service
public class InventoryTransactionService implements BaseRepository<InventoryTransaction, InventoryTransactionQuery> {

    @Autowired
    private InventoryTransactionRepository transactionRepository; // 库存变动记录仓库，用于与数据库交互

    @Autowired
    private JPAQueryFactory queryFactory; // JPA查询工厂


    @Override
    public JPAQuery<InventoryTransaction> buildConditionQuery(InventoryTransactionQuery query) {
        QInventoryTransaction qTransaction = QInventoryTransaction.inventoryTransaction; // 查询库存变动记录的QueryDSL对象
        
        JPAQuery<InventoryTransaction> jpaQuery = queryFactory
                .selectFrom(qTransaction)
                .distinct();

        // 处理查询条件
        BooleanBuilder where = new BooleanBuilder();

        if (query.getProductId() != null) {
            where.and(qTransaction.product.id.eq(query.getProductId()));
        }

        if (query.getBatchId() != null) {
            where.and(qTransaction.batch.id.eq(query.getBatchId()));
        }

        // 注意: 需要在 InventoryTransactionQuery 中添加 getPurchaseId() 和 getReturnOrderId() 方法
        if (query.getOrderId() != null) { // 假设 OrderId 对应销售订单
            where.and(qTransaction.order.id.eq(query.getOrderId()));
        } else if (query.getPurchaseId() != null) {
            where.and(qTransaction.purchase.id.eq(query.getPurchaseId()));
        } else if (query.getReturnOrderId() != null) {
            where.and(qTransaction.returnOrder.id.eq(query.getReturnOrderId()));
        }

        if (query.getOperationType() != null) {
            where.and(qTransaction.operationType.eq(query.getOperationType()));
        }

        if (query.getStartTime() != null && query.getEndTime() != null) {
            where.and(qTransaction.transactionTime.between(query.getStartTime(), query.getEndTime()));
        }

        return jpaQuery
                .where(where)
                .orderBy(qTransaction.transactionTime.desc());
    }

    @Override
    public void buildRelationship(InventoryTransactionQuery query, JPAQuery<InventoryTransaction> jpaQuery) {
        QInventoryTransaction qTransaction = QInventoryTransaction.inventoryTransaction; // 查询库存变动记录的QueryDSL对象
        QProduct qProduct = QProduct.product; // 查询产品的QueryDSL对象
        QBatch qBatch = QBatch.batch; // 查询批次的QueryDSL对象
        QOrder qOrder = QOrder.order; // 查询销售订单的QueryDSL对象
        QPurchase qPurchase = QPurchase.purchase; // 查询采购订单的QueryDSL对象
        QReturnOrder qReturnOrder = QReturnOrder.returnOrder; // 查询退货订单的QueryDSL对象

        // 处理关联
        if (query.getIncludes()
                 .contains(InventoryTransactionQuery.Include.PRODUCT)) {
            jpaQuery.leftJoin(qTransaction.product, qProduct)
                    .fetchJoin();
        }
        if (query.getIncludes()
                 .contains(InventoryTransactionQuery.Include.BATCH)) {
            jpaQuery.leftJoin(qTransaction.batch, qBatch)
                    .fetchJoin();
        }
        // 注意: 需要在 InventoryTransactionQuery.Include 中添加 PURCHASE 和 RETURN_ORDER
        if (query.getIncludes().contains(InventoryTransactionQuery.Include.ORDER)) { // 销售订单
            jpaQuery.leftJoin(qTransaction.order, qOrder).fetchJoin();
        }
        if (query.getIncludes().contains(InventoryTransactionQuery.Include.PURCHASE)) { // 采购订单
            jpaQuery.leftJoin(qTransaction.purchase, qPurchase).fetchJoin();
        }
        if (query.getIncludes().contains(InventoryTransactionQuery.Include.RETURN_ORDER)) { // 退货订单
            jpaQuery.leftJoin(qTransaction.returnOrder, qReturnOrder).fetchJoin();
        }
    }

    // --- 记录库存变动方法重构 ---

    /**
     * 核心私有方法：创建并保存库存变动记录
     */
    private void createAndSaveTransaction(Product product, Batch batch, Integer quantity,
                                          OperationType operationType, Order salesOrder,
                                          Purchase purchaseOrder, ReturnOrder returnOrder) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProduct(product);
        transaction.setBatch(batch);
        transaction.setQuantity(quantity);
        transaction.setOperationType(operationType);
        transaction.setOrder(salesOrder); // 关联销售订单
        transaction.setPurchase(purchaseOrder); // 关联采购订单
        transaction.setReturnOrder(returnOrder); // 关联退货订单
        transaction.setTransactionTime(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    /**
     * 记录库存变动（不关联任何订单）
     */
    @Transactional
    public void recordTransaction(Product product, Batch batch, Integer quantity, OperationType operationType) {
        createAndSaveTransaction(product, batch, quantity, operationType, null, null, null);
    }

    /**
     * 记录库存变动（关联销售订单）
     */
    @Transactional
    public void recordTransactionForSales(Product product, Batch batch, Integer quantity,
                                          OperationType operationType, Order salesOrder) {
        createAndSaveTransaction(product, batch, quantity, operationType, salesOrder, null, null);
    }

    /**
     * 记录库存变动（关联采购订单）
     */
    @Transactional
    public void recordTransactionForPurchase(Product product, Batch batch, Integer quantity,
                                             OperationType operationType, Purchase purchaseOrder) {
        createAndSaveTransaction(product, batch, quantity, operationType, null, purchaseOrder, null);
    }

    /**
     * 记录库存变动（关联退货订单）
     */
    @Transactional
    public void recordTransactionForReturn(Product product, Batch batch, Integer quantity,
                                           OperationType operationType, ReturnOrder returnOrder) {
        createAndSaveTransaction(product, batch, quantity, operationType, null, null, returnOrder);
    }

    // --- 原有的 recordTransaction(..., Order order) 方法现在由 recordTransactionForSales 替代 ---
    // --- 原有的 recordTransaction(...) 无订单方法保持不变 ---

    // --- 查询方法保持不变，但需要注意 Repository 可能需要添加新方法 ---

    /**
     * 查询商品的库存变动历史
     *
     * @param productId 商品ID
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 库存变动记录列表
     */
    public List<InventoryTransaction> getTransactionHistory(Integer productId,
                                                            LocalDateTime startTime,
                                                            LocalDateTime endTime) {
        if (startTime != null && endTime != null) {
            return transactionRepository.findByProductIdAndTransactionTimeBetweenOrderByTransactionTimeDesc(
                    productId, startTime, endTime);
        }
        return transactionRepository.findByProductIdOrderByTransactionTimeDesc(productId);
    }

    /**
     * 查询批次的库存变动历史
     *
     * @param batchId 批次ID
     * @return 库存变动记录列表
     */
    public List<InventoryTransaction> getBatchTransactionHistory(Integer batchId) {
        return transactionRepository.findByBatchIdOrderByTransactionTimeDesc(batchId);
    }

    /**
     * 获取商品在指定时间段内的库存变动汇总
     *
     * @param productId 商品ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 按操作类型汇总的数量
     */
    public Map<OperationType, Integer> getTransactionSummary(Integer productId,
                                                             LocalDateTime startTime,
                                                             LocalDateTime endTime) {
        List<InventoryTransaction> transactions = getTransactionHistory(productId, startTime, endTime);
        return transactions.stream()
                           .collect(Collectors.groupingBy(
                                   InventoryTransaction::getOperationType,
                                   Collectors.summingInt(InventoryTransaction::getQuantity)
                           ));
    }

    /**
     * 获取商品最近的库存变动记录
     *
     * @param productId 商品ID
     * @param limit     记录数量限制
     * @return 最近的库存变动记录列表
     */
    public List<InventoryTransaction> getRecentTransactions(Integer productId, int limit) {
        return transactionRepository.findTopNByProductIdOrderByTransactionTimeDesc(productId, limit);
    }
} 