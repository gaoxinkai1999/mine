package com.example.domain.purchase.service;

import com.example.domain.batch.entity.Batch;
import com.example.domain.batch.entity.QBatch;
import com.example.domain.batch.service.BatchService;
import com.example.domain.forecast.service.ForecastService;
import com.example.domain.inventory.dto.OperationType;
import com.example.domain.inventory.entity.Inventory;
import com.example.domain.inventory.service.InventoryService;
import com.example.domain.inventory.service.InventoryTransactionService;
import com.example.domain.product.dto.ProductDto;
import com.example.domain.product.entity.Product;
import com.example.domain.product.entity.QProduct;
import com.example.domain.product.service.ProductService;
import com.example.domain.purchase.dto.ProductWithPurchaseInfoDto;
import com.example.domain.purchase.dto.PurchaseCreateRequest;
import com.example.domain.purchase.dto.PurchaseDto;
import com.example.domain.purchase.entity.*;
import com.example.domain.purchase.mapper.PurchaseMapper;
import com.example.domain.purchase.repository.PurchaseRepository;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.domain.statistics.service.StatisticsService;
import com.example.exception.MyException;
import com.example.interfaces.BaseRepository;
import com.example.query.InventoryQuery;
import com.example.query.ProductQuery;
import com.example.query.PurchaseQuery;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 采购管理服务
 * 处理采购订单的创建、查询和管理
 */
@Service
@Slf4j
public class PurchaseService implements BaseRepository<Purchase, PurchaseQuery> {

    @Autowired
    private InventoryService inventoryService; // 库存服务，用于管理库存

    @Autowired
    private PurchaseRepository purchaseRepository; // 采购仓库，用于与数据库交互

    @Autowired
    private ProductService productService; // 产品服务

    @Autowired
    private StatisticsService statisticsService; // 统计服务

    @Autowired
    private BatchService batchService; // 批次服务

    @Autowired
    private ForecastService forecastService; // 预测服务

    @Autowired
    private JPAQueryFactory queryFactory; // JPA查询工厂

    @Autowired
    private PurchaseMapper purchaseMapper; // 采购映射器，用于对象转换

    @Autowired
    private InventoryTransactionService inventoryTransactionService; // 注入库存交易服务

    /**
     * 根据查询条件构建基本条件查询
     *
     * @param query 查询条件
     * @return 构建好的基本查询对象
     */
    @Override
    public JPAQuery<Purchase> buildConditionQuery(PurchaseQuery query) {
        QPurchase qPurchase = QPurchase.purchase;
        QPurchaseDetail qPurchaseDetail = QPurchaseDetail.purchaseDetail;

        // 创建一个选择所有字段的查询，并确保结果唯一
        JPAQuery<Purchase> jpaQuery = queryFactory.selectFrom(qPurchase)
                                                  .distinct();

        // 构建查询条件
        BooleanBuilder where = new BooleanBuilder();
        if (query.getIds() != null) {
            where.and(qPurchase.id.in(query.getIds()));
        }

        if (query.getId() != null) {
            where.and(qPurchase.id.eq(query.getId()));
        }

        if (query.getState() != null) {
            where.and(qPurchase.state.stringValue()
                                     .eq(query.getState()));
        }

        if (query.getCreateTimeStart() != null) {
            where.and(qPurchase.createTime.goe(query.getCreateTimeStart()));
        }

        if (query.getCreateTimeEnd() != null) {
            where.and(qPurchase.createTime.loe(query.getCreateTimeEnd()));
        }

        return jpaQuery.where(where)
                       .orderBy(qPurchase.createTime.desc());
    }

    /**
     * 根据查询条件构建关联加载
     *
     * @param query    查询条件
     * @param jpaQuery 已构建的基本查询对象
     */
    @Override
    public void buildRelationship(PurchaseQuery query, JPAQuery<Purchase> jpaQuery) {
        QPurchase qPurchase = QPurchase.purchase;
        QPurchaseDetail qPurchaseDetail = QPurchaseDetail.purchaseDetail;
        QProduct qProduct = QProduct.product;

        // 处理关联
        if (query.getIncludes() != null && query.getIncludes()
                                                .contains(PurchaseQuery.Include.PURCHASE_DETAILS)) {
            jpaQuery.leftJoin(qPurchase.purchaseDetails, qPurchaseDetail)
                    .fetchJoin();

            if (query.getIncludes()
                     .contains(PurchaseQuery.Include.PRODUCT)) {
                jpaQuery.leftJoin(qPurchaseDetail.product, qProduct)
                        .fetchJoin();
            }
            if (query.getIncludes()
                     .contains(PurchaseQuery.Include.BATCH)) {
                jpaQuery.leftJoin(qPurchaseDetail.batch, QBatch.batch)
                        .fetchJoin();
            }
        }
    }

    /**
     * 根据ID获取采购订单
     *
     * @param id 采购订单ID
     * @return 采购订单
     */
    public Optional<Purchase> getPurchaseById(Integer id) {
        return findOne(PurchaseQuery.builder()
                                    .id(id)
                                    .includes(Set.of(PurchaseQuery.Include.PURCHASE_DETAILS, PurchaseQuery.Include.PRODUCT))
                                    .build());
    }

    /**
     * 根据ID获取采购订单DTO
     *
     * @param id 采购订单ID
     * @return 采购订单DTO
     */
    public Optional<PurchaseDto> getPurchaseDtoById(Integer id) {
        return getPurchaseById(id)
                .map(purchaseMapper::toPurchaseDTO);
    }

    /**
     * 创建采购订单并入库
     *
     * @param request 采购订单创建请求
     */
    @Transactional
    public void createPurchaseOrder(PurchaseCreateRequest request) {
        // 1. 创建采购订单
        Purchase purchase = new Purchase();
        purchase.setState(PurchaseState.已下单);
        purchase.setPurchaseDetails(new ArrayList<>());

        // 2. 处理每个商品的采购明细
        for (PurchaseCreateRequest.PurchaseDetailRequest detailRequest : request.getDetails()) {
            // 获取商品信息
            Product product = productService.findOne(ProductQuery.builder()
                                                                 .id(detailRequest.getProductId())
                                                                 .build())
                                            .orElseThrow(() -> new MyException("商品不存在: " + detailRequest.getProductId()));


            // 创建采购明细
            PurchaseDetail detail = new PurchaseDetail();
            detail.setPurchase(purchase);
            detail.setProduct(product);
            detail.setQuantity(detailRequest.getQuantity());
            detail.setTotalAmount(detailRequest.getTotalAmount());
            purchase.getPurchaseDetails()
                    .add(detail);
        }
        purchase.setTotalAmount(purchase.getPurchaseDetails()
                                        .stream()
                                        .map(PurchaseDetail::getTotalAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 3. 保存采购订单
        purchaseRepository.save(purchase);
    }

    /**
     * 取消采购订单
     * 如果已入库则出库
     * 否则直接取消订单吗
     */
    @Transactional
    public void cancelPurchaseOrder(Integer purchaseId) {

        // 1. 查找订单
        Purchase purchase = purchaseRepository.findById(purchaseId)
                                              .orElseThrow(() -> new MyException("采购单不存在: " + purchaseId));

        // 2. 检查订单状态是否为已完成
        if (purchase.getState() == PurchaseState.已下单) {
            // 已下单逻辑，直接删除采购订单
            purchaseRepository.delete(purchase);
        } else {
            // 已下库逻辑，处理每个商品的出库，然后删除采购订单

            for (PurchaseDetail item : purchase.getPurchaseDetails()) {
                Product product = item.getProduct();

                if (product.isBatchManaged()) {
                    // 对于批次商品，需要删除对应的批次和库存记录
                    batchService.deleteBatch(item.getBatch().getId());
                    Inventory inventory = inventoryService.findOne(InventoryQuery.builder()
                                                                           .productId(product.getId())
                                                                           .batchId(item.getBatch()
                                                                                                  .getId())
                                                                           .build()).orElseThrow();
                    inventoryService.delete(inventory.getId());
                } else {
                    // 对于非批次商品，直接出库
                    inventoryService.stockOut(product, item.getQuantity());
                }
            }
            // 删除采购订单
            purchaseRepository.delete(purchase);
        }
    }

    /**
     * 获取包含采购信息的在售商品列表
     *
     * @return
     */
    public List<ProductWithPurchaseInfoDto> getOnSaleProductsWithPurchaseInfo() {
        // 在售商品的详细信息，包含库存
        List<ProductDto> products = productService.getProducts();

        // 取出商品id
        int[] array = products.stream()
                              .mapToInt(ProductDto::getId)
                              .toArray();

        Long startTime = System.currentTimeMillis();
        // 计算预计销量和预警库存
        Map<Integer, Map<String, Integer>> map = calculatePurchaseQuantity(array);
        Long endTime = System.currentTimeMillis();
        long a = endTime - startTime;
        log.info("计算预计销量和预警库存耗时：{}ms", a);

        List<ProductWithPurchaseInfoDto> list = products.stream()
                                                        .map(productDto -> {
                                                            ProductWithPurchaseInfoDto productWithPurchaseInfoDto = new ProductWithPurchaseInfoDto();
                                                            productWithPurchaseInfoDto.setCategoryId(productDto.getCategoryId());
                                                            productWithPurchaseInfoDto.setId(productDto.getId());
                                                            productWithPurchaseInfoDto.setName(productDto.getName());
                                                            productWithPurchaseInfoDto.setPurchasePrice(productDto.getCostPrice());
                                                            productWithPurchaseInfoDto.setCurrentStock(productDto.getProductStockDTO()
                                                                                                                 .getTotalInventory());

                                                            map.entrySet()
                                                               .stream()
                                                               .filter(entry -> entry.getKey() == productDto.getId())
                                                               .findFirst()
                                                               .map(Map.Entry::getValue)
                                                               .ifPresentOrElse(
                                                                       v -> {
                                                                           productWithPurchaseInfoDto.setWarningQuantity(v.get("warningQuantity"));
                                                                           productWithPurchaseInfoDto.setRecommendPurchaseQuantity(v.get("forecastQuantity") - productWithPurchaseInfoDto.getCurrentStock());
                                                                       },
                                                                       () -> productWithPurchaseInfoDto.setForecastNormal(false)
                                                               );


                                                            return productWithPurchaseInfoDto;
                                                        })
                                                        .toList();

        return list;
    }

    /**
     * 计算指定商品列表的预计销量和预警库存。
     *
     * @param productIds 商品ID数组。
     * @return Map，键为商品ID，值为包含 "forecastQuantity" (预计销量) 和 "warningQuantity" (预警库存) 的 Map。
     * 如果某个商品预测失败，其对应的值可能为 null 或包含错误信息（取决于具体实现）。
     */
    public Map<Integer, Map<String, Integer>> calculatePurchaseQuantity(int[] productIds) {
        Map<Integer, Map<String, Integer>> productIdQuantityMap = new HashMap<>();
        int forecastHorizonDays = 30; // 预测未来多少天
        int warningStockDays = 5;    // 预警库存覆盖的天数

        // 1. 获取足够长的历史销售数据 (例如过去 1 年 + 预测期，以便季节性模型有足够数据)
        //    注意：具体需要多长的数据取决于最复杂模型的需求 (HoltWintersSeasonal 需要至少 1 年)
        //    这里获取稍长一些的数据以备优化和初始化使用
        LocalDate endDate = LocalDate.now()
                                     .minusDays(1);
        LocalDate startDate = endDate.minusDays(230); // 获取约1年多的数据
        log.info("开始计算采购数量，获取历史数据范围: {} 到 {}", startDate, endDate);
        Map<LocalDate, SalesStatisticsDTO> historicalDataMap = statisticsService.calculateDailyStatistics(startDate, endDate);
        log.info("获取到 {} 天的历史销售统计数据。", historicalDataMap.size());

        // 2. 使用自定义线程池的异步方式预测所有商品，提高效率并控制资源使用
        List<CompletableFuture<Map.Entry<Integer, double[]>>> futures = new ArrayList<>();
        
        // 为每个商品创建一个异步任务，使用自定义线程池
        for (int productId : productIds) {
            CompletableFuture<double[]> forecastFuture = forecastService.forecastProductTotalWithCustomExecutor(
                productId, 
                historicalDataMap, 
                forecastHorizonDays
            );
            
            // 将结果和商品ID关联起来
            CompletableFuture<Map.Entry<Integer, double[]>> resultFuture = forecastFuture.thenApply(
                forecast -> new AbstractMap.SimpleEntry<>(productId, forecast)
            );
            
            futures.add(resultFuture);
            log.debug("已提交商品ID: {} 的异步预测任务", productId);
        }
        
        // 等待所有异步任务完成并收集结果
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // 处理每个预测结果
        for (CompletableFuture<Map.Entry<Integer, double[]>> future : futures) {
            try {
                Map.Entry<Integer, double[]> entry = future.get();
                int productId = entry.getKey();
                double[] forecast = entry.getValue();
                
                // 计算预测总销量
                double totalForecastQuantity = Arrays.stream(forecast).sum();
                log.info("商品ID: {} 预测未来 {} 天总销量: {}", productId, forecastHorizonDays, totalForecastQuantity);
                
                // 计算预警库存（第一天的预测销量）
                double warningStock = Arrays.stream(forecast).limit(warningStockDays).sum();
                log.info("商品ID: {} 预警库存: {}", productId, warningStock);
                
                // 存储结果 (确保为整数)
                productIdQuantityMap.put(productId, Map.of(
                        "forecastQuantity", (int) Math.round(totalForecastQuantity),
                        "warningQuantity", (int) Math.round(warningStock)
                ));
            } catch (Exception e) {
                // 处理异常情况
                int productId = -1;
                try {
                    // 尝试获取商品ID（如果可能的话）
                    productId = future.get().getKey();
                } catch (Exception ex) {
                    log.error("无法获取预测失败商品的ID: {}", ex.getMessage());
                }
                
                if (productId != -1) {
                    log.error("商品ID: {} 预测过程中发生错误: {}", productId, e.getMessage());
                    productIdQuantityMap.put(productId, null);
                }
            }
        }

        log.info("采购数量计算完成，成功预测 {} 个商品。", productIdQuantityMap.values()
                                                                            .stream()
                                                                            .filter(v -> v != null)
                                                                            .count());
        return productIdQuantityMap;
    }

    /**
     * 采购单入库处理
     * 将采购单状态从"已下单"更新为"已入库"，并处理库存入库
     *
     * @param purchaseId 采购单ID
     * @param batchInfoList 批次信息列表，包含生产日期和过期日期
     */
    @Transactional
    public void processPurchaseInStock(Integer purchaseId, List<PurchaseBatchInfo> batchInfoList) {
        // 1. 查找采购单
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new MyException("采购单不存在: " + purchaseId));

        // 2. 检查采购单状态
        if (purchase.getState() != PurchaseState.已下单) {
            throw new MyException("只有已下单状态的采购单可以入库");
        }

        // 3. 如果提供了批次信息，先进行验证
        Map<Integer, PurchaseBatchInfo> batchInfoMap = new HashMap<>();
        if (batchInfoList != null && !batchInfoList.isEmpty()) {
            batchInfoList.forEach(info -> batchInfoMap.put(info.getPurchaseDetailId(), info));
        }

        // 4. 处理每个采购明细的入库
        for (PurchaseDetail detail : purchase.getPurchaseDetails()) {
            Product product = detail.getProduct();
            
            if (product.isBatchManaged()) {
                // 批次管理的商品
                PurchaseBatchInfo batchInfo = batchInfoMap.get(detail.getId());
                if (batchInfo == null) {
                    throw new MyException("批次管理商品必须提供批次信息: " + product.getName());
                }
                
                // 创建批次
                Batch batch = batchService.createBatch(
                    product, 
                    detail, 
                    batchInfo.getProductionDate(), 
                    batchInfo.getExpirationDate()
                );
                
                // 关联批次到采购明细
                detail.setBatch(batch);
                
                // 入库
                inventoryService.stockIn(product, batch, detail.getQuantity());
                // 记录库存流水
                inventoryTransactionService.recordTransactionForPurchase(product, batch, detail.getQuantity(), OperationType.采购入库, purchase);
            } else {
                // 非批次管理的商品直接入库
                inventoryService.stockIn(product, detail.getQuantity());
                // 记录库存流水
                inventoryTransactionService.recordTransactionForPurchase(product, null, detail.getQuantity(), OperationType.采购入库, purchase);
            }
        }

        // 5. 更新采购单状态和入库时间
        purchase.setState(PurchaseState.已入库);
        purchase.setInTime(LocalDateTime.now());
        
        // 6. 保存更新
        purchaseRepository.save(purchase);
    }

    /**
     * 批次信息内部类
     */
    @Data
    public static class PurchaseBatchInfo {
        private Integer purchaseDetailId; // 采购明细ID
        private LocalDate productionDate; // 生产日期
        private LocalDate expirationDate; // 过期日期
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductBudgetInfo {
        private int productId;
        private BigDecimal unitPrice;
        private int currentStock;
        private String productName;

    }

    /**
     * 根据总预算和各商品的预测销量及库存情况，智能分配预算，计算每个商品的建议采购数量。
     * <p>
     * 核心逻辑：
     * 1. 获取较长时间范围内的历史销售数据，供预测模型使用。
     * 2. 对每个商品进行未来若干天（默认4天）的销量预测。
     * 3. 结合当前库存，计算每个商品的"缺口销量" = max(预测总销量 - 当前库存, 0)，表示建议补货的数量。
     * 4. 统计所有商品的缺口销量总和，计算每个商品的缺口权重 = 该商品缺口 / 总缺口。
     * 5. 按照权重，将总预算拆分分配给各商品，预算越多表示该商品越紧缺。
     * 6. 根据每个商品的单价，将分配的预算转化为采购数量，向下取整保证不超预算。
     * 7. 返回每个商品对应的采购数量，预算尽量用完，库存充足或预测销量低的商品可能为0。
     *
     * @param totalBudget 总采购预算金额，单位：元
     * @param productInfoList 商品信息列表，包含商品ID、单价、当前库存、商品名称等
     * @return 商品ID与建议采购数量的映射关系，key为商品ID，value为采购数量（整数，可能为0）
     */
    public Map<Integer, Integer> allocateBudgetForProducts(BigDecimal totalBudget, List<ProductBudgetInfo> productInfoList) {
        // 最终结果：每个商品的建议采购数量
        Map<Integer, Integer> result = new HashMap<>();

        // 预测未来几天的销量，默认4天
        int forecastDays = 4;
        // 计算历史销售数据的时间范围
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(230);
        // 获取历史销售数据，供预测模型使用
        Map<LocalDate, SalesStatisticsDTO> historicalDataMap = statisticsService.calculateDailyStatistics(startDate, endDate);

        // 使用自定义线程池进行并发预测，提高效率并控制资源使用
        Map<Integer, CompletableFuture<double[]>> forecastFutures = new HashMap<>();
        // 为每个商品创建异步预测任务
        for (ProductBudgetInfo info : productInfoList) {
            // 使用自定义线程池而不是@Async注解
            forecastFutures.put(
                info.getProductId(),
                forecastService.forecastProductTotalWithCustomExecutor(
                    info.getProductId(),
                    historicalDataMap,
                    forecastDays
                )
            );
            log.debug("已提交商品[{}]的预测任务", info.getProductName());
        }
        
        // 等待所有预测完成
        CompletableFuture.allOf(forecastFutures.values().toArray(new CompletableFuture[0])).join();
        log.info("所有预测任务已完成，开始处理结果");
        
        // 存储每个商品的缺口销量
        Map<Integer, Double> productGapMap = new HashMap<>();
        // 所有商品的缺口销量总和
        double totalGap = 0.0;

        // 遍历每个商品，处理预测结果并计算缺口
        for (ProductBudgetInfo info : productInfoList) {
            try {
                // 获取预测结果
                double[] forecast = forecastFutures.get(info.getProductId()).get();
                // 计算预测期内的总销量
                double forecastSum = Arrays.stream(forecast).sum();
                // 计算缺口销量 = max(预测销量 - 当前库存, 0)，避免负值
                double gap = Math.max(forecastSum - info.getCurrentStock(), 0);
                productGapMap.put(info.getProductId(), gap);
                totalGap += gap;
                
                log.debug("商品[{}]: 预测销量={}, 当前库存={}, 缺口={}", 
                    info.getProductName(), forecastSum, info.getCurrentStock(), gap);
            } catch (Exception e) {
                // 预测失败时，缺口视为0
                log.error("商品[{}]预测失败: {}", info.getProductName(), e.getMessage());
                productGapMap.put(info.getProductId(), 0.0);
            }
        }

        // 如果所有商品都没有缺口，直接返回空结果
        if (totalGap <= 0) {
            log.info("所有商品库存充足，无需采购");
            return result;
        }

        // 根据缺口销量占比，分配预算并计算采购数量
        for (ProductBudgetInfo info : productInfoList) {
            double gap = productGapMap.getOrDefault(info.getProductId(), 0.0);
            if (gap <= 0) {
                // 缺口为0时，不分配预算
                result.put(info.getProductId(), 0);
                continue;
            }
            // 当前商品分配的预算 = 总预算 * 缺口占比
            BigDecimal productBudget = totalBudget.multiply(BigDecimal.valueOf(gap / totalGap));
            // 采购数量 = 预算 / 单价，向下取整避免超预算
            int quantity = productBudget.divide(info.getUnitPrice(), 0, BigDecimal.ROUND_DOWN).intValue();
            result.put(info.getProductId(), quantity);
            
            log.debug("商品[{}]: 分配预算={}, 建议采购数量={}", 
                info.getProductName(), productBudget, quantity);
        }

        // 返回每个商品的采购数量
        return result;
    }
}
