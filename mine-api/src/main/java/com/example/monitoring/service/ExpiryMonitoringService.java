package com.example.monitoring.service; // Updated package

import com.example.domain.order.repository.OrderRepository; // 保持 OrderRepository 依赖用于执行 Native Query
import com.example.monitoring.dto.ProcessedItemDetailDto; // 导入新的 DTO
import com.example.monitoring.repository.ExpiryAlertProcessedLogRepository; // 导入新的 Repository
import org.springframework.data.domain.Page; // 导入 Page
import org.springframework.data.domain.Pageable; // 导入 Pageable
// 移除不再需要的实体类导入
// import com.example.domain.product.entity.Product;
// import com.example.domain.batch.entity.Batch;
// import com.example.domain.order.entity.OrderDetail;
// import com.example.domain.order.entity.SaleBatchDetail;
// import com.example.domain.shop.entity.Shop;
// 移除不再需要的 Repository 和 Service
// import com.example.domain.shop.service.ShopService;
// import com.example.query.ShopQuery;
// import com.example.domain.order.repository.SaleBatchDetailRepository;
import com.example.monitoring.dto.NearExpiryBatchInfo; // DTO import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 移除不再需要的日期时间类
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.time.temporal.ChronoUnit;
import java.util.List;
// import java.util.ArrayList; // 不再需要手动创建列表
// import java.util.Optional; // 不再需要 Optional

/**
 * 商品临期监控服务 (已重构为使用 Native SQL 查询)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiryMonitoringService {

    private final OrderRepository orderRepository; // 依赖 OrderRepository 执行 Native Query
    private final ExpiryAlertProcessedLogRepository expiryAlertProcessedLogRepository; // 注入新的 Repository


    /**
     * 查找临期批次信息 (使用 Native SQL 查询)
     * 监控逻辑：直接通过 SQL 查询数据库，找出所有满足以下条件的批次：
     * 1. 属于未删除的商家。
     * 2. 商品是批次管理的。
     * 3. 批次有生产日期。
     * 4. 批次生产日期距今天数已达到或超过商品的临期监控阈值。
     * 5. 该批次关联的订单是对应商家、对应商品的最新一个满足临期条件的订单。
     *
     * @return 临期批次信息列表 (NearExpiryBatchInfo DTO 列表)
     */
    @Transactional(readOnly = true)
    public List<NearExpiryBatchInfo> findNearExpiryBatches() {
        log.info("开始执行临期批次监控查询 (Native SQL)...");

        List<NearExpiryBatchInfo> nearExpiryBatches = orderRepository.findNearExpiryBatchInfoNative();

        return nearExpiryBatches;
    }

    /**
     * 获取已处理的临期商品项历史记录
     *
     * @param pageable 分页参数
     * @return 分页的已处理商品项详情
     */
    @Transactional(readOnly = true)
    public Page<ProcessedItemDetailDto> getProcessedItemHistory(Pageable pageable) {
        log.info("查询已处理的临期商品历史记录，分页参数: {}", pageable);
        Page<ExpiryAlertProcessedLogRepository.ProcessedItemDetailDtoProjection> projectionPage =
                expiryAlertProcessedLogRepository.findProcessedItemHistory(pageable);

        return projectionPage.map(projection -> new ProcessedItemDetailDto(
                projection.getSaleBatchDetailId(),
                projection.getShopName(),
                projection.getProductName(),
                projection.getOrderId(),
                projection.getOrderDetailId(),
                projection.getBatchId(),
                (projection.getProductionDate() != null ? projection.getProductionDate().toLocalDate() : null),
                (projection.getProcessedAt() != null ? projection.getProcessedAt().toLocalDateTime() : null)
        ));
    }
}