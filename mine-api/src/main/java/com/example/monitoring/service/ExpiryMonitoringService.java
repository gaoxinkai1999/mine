package com.example.monitoring.service; // Updated package

import com.example.domain.order.repository.OrderRepository;
import com.example.domain.product.entity.Product;
// import com.example.domain.product.repository.ProductRepository; // Not used
import com.example.domain.batch.entity.Batch;
import com.example.domain.order.entity.OrderDetail;
import com.example.domain.order.entity.SaleBatchDetail;
import com.example.domain.order.repository.SaleBatchDetailRepository;
import com.example.domain.shop.entity.Shop;
import com.example.domain.shop.service.ShopService;
import com.example.query.ShopQuery;
import com.example.monitoring.dto.NearExpiryBatchInfo; // Updated DTO import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 商品临期监控服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiryMonitoringService {

    // 移除 ProductRepository 依赖，因为我们不再从商品开始查询
    // private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ShopService shopService; // 用于获取商家信息
    private final SaleBatchDetailRepository saleBatchDetailRepository; // 注入 SaleBatchDetailRepository

    /**
     * 查找临期批次信息
     * 监控逻辑：对于每个商家，找到其最后一次包含批次管理商品的销售订单，
     * 然后检查该订单中每个批次的生产日期是否超过对应商品的临期阈值。
     *
     * @return 临期批次信息列表
     */
    @Transactional(readOnly = true)
    public List<NearExpiryBatchInfo> findNearExpiryBatches() {
        log.info("开始执行临期批次监控查询...");
        List<NearExpiryBatchInfo> nearExpiryBatches = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 1. 获取所有未删除的商家
        List<Shop> allShops = shopService.findList(ShopQuery.builder().del(false).build());
        if (allShops.isEmpty()) {
             log.warn("系统中没有查询到任何商家信息，无法进行临期监控。");
             return nearExpiryBatches;
        }
        log.debug("查询到 {} 个商家", allShops.size());

        // 2. 遍历每个商家
        for (Shop shop : allShops) {
            log.debug("检查商家: {} (ID: {})", shop.getName(), shop.getId());

            // 3. 查找该商家包含批次管理商品的最新订单时间
            //    假设 OrderRepository 有此方法: findLatestOrderTimeWithBatchManagedProductByShop(shopId)
            Optional<LocalDateTime> latestOrderTimeOpt = orderRepository.findLatestOrderTimeWithBatchManagedProductByShop(shop.getId());

            if (latestOrderTimeOpt.isPresent()) {
                LocalDateTime latestOrderTime = latestOrderTimeOpt.get();
                log.debug("商家 {} 最新相关订单时间: {}", shop.getName(), latestOrderTime);

                // 4. 查找在该精确时间点，该商家的所有订单ID (可能存在同一时间多笔订单)
                //    假设 OrderRepository 有此方法: findOrderIdsByShopIdAndCreateTime(shopId, createTime)
                List<Integer> latestOrderIds = orderRepository.findOrderIdsByShopIdAndCreateTime(shop.getId(), latestOrderTime);
                if (latestOrderIds.isEmpty()) {
                    log.warn("找到了最新订单时间 {} 但未能找到对应的订单ID，跳过商家 {}", latestOrderTime, shop.getName());
                    continue;
                }
                log.debug("商家 {} 最新相关订单ID: {}", shop.getName(), latestOrderIds);


                // 5. 查询这些最新订单关联的所有销售批次详情 (SaleBatchDetail)
                //    需要确保关联查询 Batch 和 Product 以获取所需信息
                //    假设 SaleBatchDetailRepository 有此方法: findWithDetailsByOrderIds(orderIds)
                List<SaleBatchDetail> saleBatchDetails = saleBatchDetailRepository.findWithDetailsByOrderIds(latestOrderIds);
                log.debug("商家 {} 最新相关订单共找到 {} 个销售批次详情", shop.getName(), saleBatchDetails.size());


                // 6. 遍历每个销售批次详情，进行临期判断
                for (SaleBatchDetail sbd : saleBatchDetails) {
                    Batch batch = sbd.getBatch();
                    OrderDetail orderDetail = sbd.getOrderDetail();
                    Product product = orderDetail.getProduct(); // 获取关联的商品

                    // 确保获取到了必要的信息
                    if (batch == null || product == null) {
                        log.warn("销售批次详情 ID: {} 缺少关联的 Batch 或 Product 信息，跳过。", sbd.getId());
                        continue;
                    }

                    LocalDate productionDate = batch.getProductionDate();
                    Integer thresholdDays = product.getExpiryMonitoringThresholdDays();

                    // 检查生产日期和阈值是否有效
                    if (productionDate != null && thresholdDays != null && thresholdDays > 0) {
                        // 计算生产距今天数
                        long daysSinceProduction = ChronoUnit.DAYS.between(productionDate, today);

                        // 判断是否超过阈值
                        if (daysSinceProduction >= thresholdDays) {
                            log.info("发现临期批次: Shop='{}'(ID={}), Product='{}'(ID={}), Batch='{}'(ID={}), ProductionDate={}, Days={}, Threshold={}",
                                      shop.getName(), shop.getId(), product.getName(), product.getId(),
                                      batch.getBatchNumber(), batch.getId(), productionDate, daysSinceProduction, thresholdDays);

                            // 创建 DTO 并添加到结果列表
                            NearExpiryBatchInfo info = new NearExpiryBatchInfo(
                                    shop.getName(),
                                    product.getName(),
                                    batch.getBatchNumber(),
                                    productionDate,
                                    daysSinceProduction,
                                    thresholdDays,
                                    latestOrderTime, // 使用之前查到的该商家的最新相关订单时间
                                    shop.getId(),
                                    product.getId(),
                                    batch.getId()
                            );
                            nearExpiryBatches.add(info);
                        }
                    } else {
                         log.trace("批次 {} (商品 {}) 无有效生产日期或阈值，跳过临期检查。", batch.getBatchNumber(), product.getName());
                    }
                }
            } else {
                 log.debug("商家 {} (ID: {}) 没有包含批次管理商品的销售订单。", shop.getName(), shop.getId());
            }
        }

        log.info("临期批次监控查询完成，发现 {} 条临期记录。", nearExpiryBatches.size());
        return nearExpiryBatches;
    }
}