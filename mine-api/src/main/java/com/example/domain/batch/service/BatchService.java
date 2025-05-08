package com.example.domain.batch.service;

import com.example.domain.batch.dto.BatchUpdateDto;
import com.example.domain.batch.entity.Batch;
import com.example.domain.batch.entity.QBatch;
import com.example.domain.batch.mapper.BatchMapper;
import com.example.domain.batch.repository.BatchRepository;
import com.example.domain.product.entity.Product;
import com.example.domain.product.entity.QProduct;
import com.example.domain.purchase.entity.PurchaseDetail;
import com.example.exception.MyException;
import com.example.interfaces.BaseRepository;
import com.example.query.BatchQuery;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 批次管理服务
 * 处理商品批次的创建、查询和状态管理
 */
@Service
public class BatchService implements BaseRepository<Batch, BatchQuery> {

    @Autowired
    private BatchRepository batchRepository; // 批次仓库，用于与数据库交互



    @Autowired
    private JPAQueryFactory queryFactory; // JPA查询工厂

    @Autowired
    private BatchMapper batchMapper; // 批次映射器，用于对象转换



    @Override
    public JPAQuery<Batch> buildConditionQuery(BatchQuery query) {
        QBatch qBatch = QBatch.batch; // 查询批次的QueryDSL对象
        
        // 初始化查询对象
        JPAQuery<Batch> jpaQuery = queryFactory
                .selectFrom(qBatch)
                .distinct();

        // 处理查询条件
        BooleanBuilder where = new BooleanBuilder();

        if (query.getId() != null) {
            where.and(qBatch.id.eq(query.getId()));
        }

        if (query.getProductId() != null) {
            where.and(qBatch.product.id.eq(query.getProductId()));
        }

        if (query.getBatchNumber() != null) {
            where.and(qBatch.batchNumber.eq(query.getBatchNumber()));
        }

        if (query.getStatus() != null) {
            where.and(qBatch.status.eq(query.getStatus()));
        }

        if (query.getExpirationDateStart() != null) {
            where.and(qBatch.expirationDate.goe(query.getExpirationDateStart()));
        }

        if (query.getExpirationDateEnd() != null) {
            where.and(qBatch.expirationDate.loe(query.getExpirationDateEnd()));
        }

        return jpaQuery.where(where).orderBy(qBatch.createdTime.desc());
    }

    @Override
    public void buildRelationship(BatchQuery query, JPAQuery<Batch> jpaQuery) {
        QBatch qBatch = QBatch.batch; // 查询批次的QueryDSL对象
        QProduct qProduct = QProduct.product; // 查询产品的QueryDSL对象

        // 处理关联
        if (query.getIncludes() != null && query.getIncludes().contains(BatchQuery.Include.PRODUCT)) {
            jpaQuery.leftJoin(qBatch.product, qProduct).fetchJoin();
        }
    }

    /**
     * 创建新批次
     */
    @Transactional
    public Batch createBatch(Product product, PurchaseDetail purchaseDetail, LocalDate productionDate, LocalDate expirationDate) {
        if (!product.isBatchManaged()) {
            throw new MyException("商品未启用批次管理: " + product.getName());
        }

        Batch batch = new Batch();
        batch.setProduct(product);
        batch.setPurchaseDetail(purchaseDetail);
        batch.setBatchNumber(generateBatchNumber(productionDate));
        batch.setProductionDate(productionDate);
        batch.setExpirationDate(expirationDate);
        batch.setCostPrice(purchaseDetail.getProduct().getCostPrice());
        batch.setStatus(true);

        return batchRepository.save(batch);
    }



    /**
     * 查询商品的有效批次
     */
    public List<Batch> findValidBatches(Integer productId) {
        return findList(BatchQuery.builder()
            .productId(productId)
            .status(true)
            .expirationDateStart(LocalDate.now())
            .build());
    }

    /**
     * 查询商品的所有批次
     */
    public List<Batch> findByProduct(Integer productId, Boolean status) {
        return findList(BatchQuery.builder()
            .productId(productId)
            .status(status)
            .build());
    }
    /**
     * 删除批次
     */
    @Transactional
    public void deleteBatch(Integer batchId) {
        batchRepository.deleteById(batchId);
    }




    /**
     * 批量更新批次信息
     *
     * @param batches 批次更新请求列表
     */
    @Transactional
    public void batchUpdate(List<BatchUpdateDto> batches) {
        for (BatchUpdateDto batchUpdateDto : batches) {
            Batch batch = batchRepository.findById(batchUpdateDto.getId())
                    .orElseThrow(() -> new MyException("批次不存在: " + batchUpdateDto.getId()));

            Batch update = batchMapper.partialUpdate(batchUpdateDto, batch);
            batchRepository.save(update);
        }
    }


    public String generateBatchNumber(LocalDate productionDate){

        return productionDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 根据商品ID查询库存中存在的批次列表
     * @param productId 商品ID
     * @return 批次列表
     */
    public List<Batch> findBatchesInStockByProductId(Integer productId) {
        return batchRepository.findBatchesInStockByProductId(productId);
    }
}