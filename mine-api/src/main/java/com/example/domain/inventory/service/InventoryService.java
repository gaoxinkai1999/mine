package com.example.domain.inventory.service;

import com.example.domain.batch.entity.Batch;
import com.example.domain.batch.entity.QBatch;
import com.example.domain.inventory.controller.InventoryController;
import com.example.domain.inventory.dto.InventoryUpdateDto;
import com.example.domain.inventory.entity.Inventory;
import com.example.domain.inventory.entity.QInventory;
import com.example.domain.inventory.mapper.InventoryMapper;
import com.example.domain.inventory.repository.InventoryRepository;
import com.example.domain.product.dto.ProductStockDTO;
import com.example.domain.product.entity.Product;
import com.example.domain.product.entity.QProduct;
import com.example.exception.MyException;
import com.example.interfaces.BaseRepository;
import com.example.query.InventoryQuery;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 库存管理服务
 * 处理商品库存的增减、查询和验证
 * {@link InventoryController}
 * {@link Inventory}
 */
@Service
@Slf4j
public class InventoryService implements BaseRepository<Inventory, InventoryQuery> {

    @Autowired
    private InventoryRepository inventoryRepository; // 库存仓库，用于与数据库交互


    @Autowired
    private JPAQueryFactory queryFactory; // JPA查询工厂

    @Autowired
    private InventoryMapper inventoryMapper; // 库存映射器，用于对象转换


    /**
     * 构建基本条件查询
     *
     * @param query 查询条件
     * @return JPA查询对象
     */
    @Override
    public JPAQuery<Inventory> buildConditionQuery(InventoryQuery query) {
        QInventory qInventory = QInventory.inventory; // 查询库存的QueryDSL对象
        
        // 初始化查询对象
        JPAQuery<Inventory> jpaQuery = queryFactory.selectFrom(qInventory)
                                                   .distinct();

        // 处理查询条件
        BooleanBuilder where = new BooleanBuilder();

        if (query.getId() != null) {
            where.and(qInventory.id.eq(query.getId()));
        }
        if (query.getProductId() != null) {
            where.and(qInventory.product.id.eq(query.getProductId()));
        }

        if (query.getBatchId() != null) {
            where.and(qInventory.batch.id.eq(query.getBatchId()));
        }

        return jpaQuery.where(where);
    }

    /**
     * 构建关联加载
     *
     * @param query 查询条件
     * @param jpaQuery 已构建的基本查询
     */
    @Override
    public void buildRelationship(InventoryQuery query, JPAQuery<Inventory> jpaQuery) {
        QInventory qInventory = QInventory.inventory; // 查询库存的QueryDSL对象
        QProduct qProduct = QProduct.product; // 查询产品的QueryDSL对象
        QBatch qBatch = QBatch.batch; // 查询批次的QueryDSL对象
        
        // 处理关联
        if (query.getIncludes()
                 .contains(InventoryQuery.Include.PRODUCT)) {
            jpaQuery.leftJoin(qInventory.product, qProduct)
                    .fetchJoin();
        }
        if (query.getIncludes()
                 .contains(InventoryQuery.Include.BATCH)) {
            jpaQuery.leftJoin(qInventory.batch, qBatch)
                    .fetchJoin();
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void update(InventoryUpdateDto inventoryUpdateDto) {
        log.info("更新库存: {}", inventoryUpdateDto);
        Inventory inventory = findOne(InventoryQuery.builder()
                                                    .productId(inventoryUpdateDto.getProductId())
                                                    .batchId(inventoryUpdateDto.getBatchId())
                                                    .build()).orElseThrow();
        inventory.setQuantity(inventoryUpdateDto.getQuantity());
        inventoryRepository.save(inventory);
    }

    /**
     * 入库（批次商品）
     *
     * @param product  商品对象
     * @param batch    批次对象
     * @param quantity 入库数量
     */
    @org.springframework.transaction.annotation.Transactional
    public void stockIn(Product product, Batch batch, Integer quantity) {
        // 2. 查找或创建库存记录
        Inventory inventory = findOrCreateInventory(product, batch);

        // 3. 更新库存
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);

        // 4. 记录库存变动 - 暂时注释掉
        // inventoryTransactionService.recordTransaction(product, batch, quantity, OperationType.采购入库);
    }

    /**
     * 入库（非批次商品）
     *
     * @param product  商品对象
     * @param quantity 入库数量
     */
    @org.springframework.transaction.annotation.Transactional
    public void stockIn(Product product, Integer quantity) {
        // 2. 查找或创建库存记录
        Inventory inventory = findOrCreateInventory(product, null);

        // 3. 更新库存
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);

        // 4. 记录库存变动 - 暂时注释掉
        // inventoryTransactionService.recordTransaction(product, null, quantity, OperationType.采购入库);
    }

    /**
     * 出库（批次商品）
     *
     * @param product  商品对象
     * @param batch    批次对象
     * @param quantity 出库数量
     */
    @org.springframework.transaction.annotation.Transactional
    public void stockOut(Product product, Batch batch, Integer quantity) {
        // 2. 查找并检查库存
        Inventory inventory = findInventoryOrThrow(product, batch);
        if (inventory.getQuantity() < quantity) {
            throw new MyException(String.format("批次商品库存不足: 商品[%s], 批次[%s], 当前库存[%d], 需要数量[%d]", product.getName(), batch.getBatchNumber(), inventory.getQuantity(), quantity));
        }

        // 3. 更新库存
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);

        // 4. 记录库存变动 - 暂时注释掉
        // inventoryTransactionService.recordTransaction(product, batch, -quantity, OperationType.销售出库);
    }

    /**
     * 出库（非批次商品）
     *
     * @param product  商品对象
     * @param quantity 出库数量
     */
    @org.springframework.transaction.annotation.Transactional
    public void stockOut(Product product, Integer quantity) {
        // 2. 查找并检查库存
        Inventory inventory = findInventoryOrThrow(product, null);
        if (inventory.getQuantity() < quantity) {
            throw new MyException(String.format("商品库存不足: 商品[%s], 当前库存[%d], 需要数量[%d]", product.getName(), inventory.getQuantity(), quantity));
        }
        // 3. 更新库存
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);

        // 4. 记录库存变动 - 暂时注释掉
        // inventoryTransactionService.recordTransaction(product, null, -quantity, OperationType.销售出库);
    }


    /**
     * 查询商品库存信息
     * 包括总库存和批次库存信息
     *
     * @param productId 商品ID
     * @return 商品库存信息DTO
     */
    public ProductStockDTO getProductStock(Integer productId) {


        // 创建返回DTO
        ProductStockDTO dto = new ProductStockDTO();
        dto.setProductId(productId);


        // 获取总库存
        List<Inventory> inventoryList = this.findList(InventoryQuery.builder()
                                                                    .productId(productId)
                                                                    .includes(Set.of(InventoryQuery.Include.BATCH))
                                                                    .build());
        Integer sumInventory = inventoryList.stream()
                                            .map(Inventory::getQuantity)
                                            .reduce(0, Integer::sum);

        dto.setTotalInventory(sumInventory);

        // 获取批次库存
        inventoryList.forEach(inventory -> {
            Batch batch = inventory.getBatch();
            if (batch != null) {
                ProductStockDTO.BatchStock batchStock = new ProductStockDTO.BatchStock();
                batchStock.setBatchId(batch.getId());
                batchStock.setBatchNumber(batch.getBatchNumber());
                batchStock.setQuantity(inventory.getQuantity());
                batchStock.setProductionDate(batch.getProductionDate());
                batchStock.setExpirationDate(batch.getExpirationDate());
                dto.getBatchStocks()
                   .add(batchStock);
            }
        });

        return dto;
    }

    /**
     * 批量查询商品库存信息
     *
     * @param productIds 商品ID列表
     * @return 商品库存信息DTO列表
     */
    public List<ProductStockDTO> getProductStocks(List<Integer> productIds) {
        return productIds.stream()
                         .map(this::getProductStock)
                         .toList();
    }


    /**
     * 查找或创建库存记录
     *
     * @param product 商品对象
     * @param batch   批次对象
     * @return 库存记录
     */
    public Inventory findOrCreateInventory(Product product, Batch batch) {
        // 对于批次管理的商品，不允许使用无批次接口创建或查询库存
        if (batch == null && product.isBatchManaged()) {
            throw new MyException("批次管理商品必须指定批次才能操作库存");
        }
        Optional<Inventory> inventoryOpt = batch != null ? inventoryRepository.findByProductIdAndBatchId(product.getId(), batch.getId()) : inventoryRepository.findByProductIdAndBatchIdIsNull(product.getId());

        if (inventoryOpt.isPresent()) {
            return inventoryOpt.get();
        }

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setBatch(batch);
        inventory.setQuantity(0);
        return inventory;
    }

    /**
     * 查找库存记录或抛出异常
     *
     * @param product 商品对象
     * @param batch   批次对象
     * @return 库存记录
     */
    private Inventory findInventoryOrThrow(Product product, Batch batch) {
        return batch != null ? inventoryRepository.findByProductIdAndBatchId(product.getId(), batch.getId())
                                                  .orElseThrow(() -> new MyException(String.format("找不到库存记录: 商品[%s], 批次[%s]", product.getName(), batch.getBatchNumber()))) : inventoryRepository.findByProductIdAndBatchIdIsNull(product.getId())
                                                                                                                                                                                                           .orElseThrow(() -> new MyException(String.format("找不到库存记录: 商品[%s]", product.getName())));
    }

    /**
     * 按FIFO原则获取商品的可用批次
     * @param product 商品
     * @param requiredQuantity 需要的数量
     * @return 批次及其可用数量的列表
     */
    public List<BatchAllocation> findAvailableBatchesByFifo(Product product, Integer requiredQuantity) {
        QInventory qInventory = QInventory.inventory;
        QBatch qBatch = QBatch.batch;

        // 查询所有有库存的批次，按生产日期升序（FIFO）
        List<Inventory> availableInventories = queryFactory
            .selectFrom(qInventory)
            .leftJoin(qInventory.batch, qBatch).fetchJoin()
            .where(
                qInventory.product.eq(product),
                qInventory.batch.isNotNull(),
                qInventory.quantity.gt(0)
            )
            .orderBy(qBatch.productionDate.asc())
            .fetch();

        List<BatchAllocation> allocations = new ArrayList<>();
        int remainingQuantity = requiredQuantity;

        // 按FIFO分配批次
        for (Inventory inventory : availableInventories) {
            if (remainingQuantity <= 0) {
                break;
            }

            int allocateQuantity = Math.min(remainingQuantity, inventory.getQuantity());
            allocations.add(new BatchAllocation(inventory.getBatch(), allocateQuantity));
            remainingQuantity -= allocateQuantity;
        }

        if (remainingQuantity > 0) {
            throw new MyException("商品库存不足: " + product.getName() + 
                ", 缺少: " + remainingQuantity );
        }

        return allocations;
    }
    /**
     * 删除库存记录
      */
    @org.springframework.transaction.annotation.Transactional
    public void delete(Integer id) {
        inventoryRepository.deleteById(id);
    }

    /**
     * 批次分配结果类
     */
    @Data
    @AllArgsConstructor
    public static class BatchAllocation {
        private Batch batch;
        private Integer quantity;
    }

}
