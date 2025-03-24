package com.example.domain.purchase.service;

import com.example.domain.batch.entity.Batch;
import com.example.domain.batch.service.BatchService;
import com.example.domain.forecast.service.ForecastService;
import com.example.domain.inventory.service.InventoryService;
import com.example.domain.product.dto.ProductDto;
import com.example.domain.product.entity.Product;
import com.example.domain.product.service.ProductService;
import com.example.domain.purchase.dto.ProductWithPurchaseInfoDto;
import com.example.domain.purchase.dto.PurchaseCreateRequest;
import com.example.domain.purchase.entity.Purchase;
import com.example.domain.purchase.entity.PurchaseDetail;
import com.example.domain.purchase.entity.PurchaseState;
import com.example.domain.purchase.repository.PurchaseRepository;
import com.example.domain.statistics.dto.response.SalesStatisticsDTO;
import com.example.domain.statistics.service.StatisticsService;
import com.example.exception.MyException;
import com.example.query.ProductQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购管理服务
 * 处理采购订单的创建、查询和管理
 */
@Service
@Slf4j
public class PurchaseService {

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


    /**
     * 创建采购订单并入库
     *
     * @param request 采购订单创建请求
     */
    @Transactional
    public void createPurchaseOrder(PurchaseCreateRequest request) {
        // 1. 创建采购订单
        Purchase purchase = new Purchase();

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

            // 处理入库
            if (product.isBatchManaged()) {
                // 对于需要批次管理的商品，创建批次并入库
                if (detailRequest.getProductionDate() == null || detailRequest.getExpirationDate() == null) {
                    throw new MyException("批次商品必须提供生产日期和有效期：" + product.getName());
                }

                // 创建批次
                Batch batch = batchService.createBatch(
                        product,
                        detail,
                        detailRequest.getProductionDate(),
                        detailRequest.getExpirationDate()
                );

                // 批次入库
                inventoryService.stockIn(product, batch, detailRequest.getQuantity());
            } else {
                // 对于不需要批次管理的商品，直接入库
                inventoryService.stockIn(product, detailRequest.getQuantity());
            }
        }

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
                    // 对于批次商品，需要从对应批次出库
                    if (item.getBatch() != null) {
                        inventoryService.stockOut(product, item.getBatch(), item.getQuantity());
                    }
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
                                                                       () -> productWithPurchaseInfoDto.setIsForecastNormal(false)
                                                               );


                                                            return productWithPurchaseInfoDto;
                                                        })
                                                        .toList();

        return list;
    }

    // 商品预警库存和预计销量
    public Map<Integer, Map<String, Integer>> calculatePurchaseQuantity(int[] productIds) {
        Map<Integer, Map<String, Integer>> productIdQuantityMap = new HashMap<>();
        Map<LocalDate, SalesStatisticsDTO> data = statisticsService.calculateDailyStatistics(LocalDate.now()
                                                                                                      .minusDays(180), LocalDate.now()
                                                                                                                                .minusDays(1));
        Arrays.stream(productIds)
              .forEach(productId -> {
                  double[] productData = forecastService.getProductData(data, productId);
                  log.info("商品{}销量数据：{}", productId, productData.length);
                  double[] doubles = forecastService.aggregateToWeeklyData(productData);
                  log.info("商品{}销量数据：{}", productId, doubles.length);
                  // 预计销量
                  double forecastData = 0;
                  try {
                      log.info("商品{}预计销量计算中...", productId);
                      forecastData = forecastService.forecast(doubles, 4);
                  } catch (MyException e) {
                      log.error("商品{}预计销量计算失败", productId);
                      productIdQuantityMap.put(productId, null);
                      return;
                  }

                  // 预警库存
                  double warningStock = (forecastData / 28) * 5;

                  productIdQuantityMap.put(productId, Map.of("forecastQuantity", (int) forecastData, "warningQuantity", (int) warningStock));
              });
        return productIdQuantityMap;
    }


}
