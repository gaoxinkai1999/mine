package com.example.domain.order.service;

import com.example.domain.batch.entity.Batch;
import com.example.domain.batch.service.BatchService;
import com.example.domain.inventory.dto.OperationType; // 添加 OperationType 导入
import com.example.domain.inventory.service.InventoryService;
import com.example.domain.inventory.service.InventoryTransactionService; // 添加 InventoryTransactionService 导入
import com.example.domain.order.dto.OrderCreateRequest;
import com.example.domain.order.entity.*;
import com.example.domain.order.repository.OrderRepository;
import com.example.domain.product.entity.Product;
import com.example.domain.product.entity.QProduct;
import com.example.domain.product.service.ProductService;
import com.example.domain.shop.entity.QShop;
import com.example.domain.shop.entity.Shop;
import com.example.domain.shop.service.ShopService;
import com.example.exception.MyException;
import com.example.interfaces.BaseRepository;
import com.example.query.BatchQuery;
import com.example.query.OrderQuery;
import com.example.query.ProductQuery;
import com.example.query.ShopQuery;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单管理服务
 * 处理订单的创建、查询和管理
 */
@Service
public class OrderService implements BaseRepository<Order, OrderQuery> {
    @Autowired
    private InventoryService inventoryService; // 库存服务，用于管理库存

    @Autowired
    private OrderRepository orderRepository; // 订单仓库，用于与数据库交互

    @Autowired
    private BatchService batchService; // 批次服务

    @Autowired
    private ProductService productService; // 产品服务

    @Autowired
    private SaleBatchDetailService saleBatchDetailService; // 销售批次详情服务

    @Autowired
    private JPAQueryFactory queryFactory; // JPA查询工厂
    @Autowired
    private ShopService shopService;

    @Autowired
    private InventoryTransactionService inventoryTransactionService; // 注入库存交易服务


    /**
     * 构建基本条件查询对象
     * 根据传入的查询参数，构建一个基本的JPAQuery对象，只包含条件查询
     *
     * @param query 订单查询参数，包含需要查询的条件
     * @return 返回一个根据查询参数配置好的JPAQuery对象
     */
    @Override
    public JPAQuery<Order> buildConditionQuery(OrderQuery query) {
        QOrder qOrder = QOrder.order; // 查询订单的QueryDSL对象

        // 初始化查询对象
        JPAQuery<Order> jpaQuery = queryFactory
                .selectFrom(qOrder)
                .distinct();

        // 处理查询条件
        // 根据查询参数构建where条件，以精确查询
        BooleanBuilder where = new BooleanBuilder();
        // 如果查询参数中包含订单ID，则添加ID查询条件
        if (query.getId() != null) {
            where.and(qOrder.id.eq(query.getId()));
        }
        if (query.getIds() != null) {
            where.and(qOrder.id.in(query.getIds()));
        }

        // 如果查询参数中包含开始时间和结束时间，则添加时间区间查询条件
        if (query.getStartTime() != null && query.getEndTime() != null) {
            where.and(qOrder.createTime.between(
                    query.getStartTime()
                         .atStartOfDay(),
                    query.getEndTime()
                         .atTime(23, 59, 59)
            ));
        }

        // 如果查询参数中包含店铺ID，则添加店铺ID查询条件
        if (query.getShopId() != null) {
            where.and(qOrder.shop.id.eq(query.getShopId()));
        }

        // 返回最终的查询对象，包含where条件和按创建时间降序排序
        return jpaQuery.where(where)
                       .orderBy(qOrder.createTime.desc());
    }

    /**
     * 构建关联加载
     * 根据传入的查询参数，为查询对象加载关联实体
     *
     * @param query 订单查询参数，包含需要包含的关联实体
     * @param jpaQuery 已构建的基本查询对象
     */
    @Override
    public void buildRelationship(OrderQuery query, JPAQuery<Order> jpaQuery) {
        QOrder qOrder = QOrder.order; // 查询订单的QueryDSL对象
        QOrderDetail qOrderDetail = QOrderDetail.orderDetail; // 查询订单详情的QueryDSL对象
        QProduct qProduct = QProduct.product; // 查询产品的QueryDSL对象
        QShop qShop = QShop.shop; // 查询商店的QueryDSL对象

        // 处理关联
        // 如果查询参数中包含店铺信息，则左连接店铺表，并根据条件进一步连接价格规则表
        if (query.getIncludes()
                 .contains(OrderQuery.Include.SHOP)) {
            jpaQuery.leftJoin(qOrder.shop, qShop)
                    .fetchJoin();
        }

        // 如果查询参数中包含订单详情，则左连接订单详情表，并根据条件进一步连接产品表
        if (query.getIncludes()
                 .contains(OrderQuery.Include.DETAILS)) {
            jpaQuery.leftJoin(qOrder.orderDetails, qOrderDetail)
                    .fetchJoin();

            if (query.getIncludes()
                     .contains(OrderQuery.Include.PRODUCT)) {
                jpaQuery.leftJoin(qOrderDetail.product, qProduct)
                        .fetchJoin();
            }
        }
    }


    /**
     * 创建新订单
     *
     * @param request 订单创建请求
     */
    @Transactional
    public void createOrder(OrderCreateRequest request) {
        // 获取店铺信息
        Shop shop = shopService.findOne(ShopQuery.builder().id(request.getShopId())
                                                 .build())
                               .orElseThrow(() -> new MyException("店铺不存在"));
        // 创建订单
        Order order = new Order();
        order.setShop(shop);

        // 处理订单项
        for (OrderCreateRequest.OrderItemRequest itemRequest : request.getItems()) {
            // 获取商品信息
            Product product = productService.findOne(ProductQuery.builder()
                                                               .id(itemRequest.getProductId())
                                                               .build())
                                           .orElseThrow(() -> new MyException("商品不存在: " + itemRequest.getProductId()));
            
            // === 开始价格校验 ===
            BigDecimal requestedPrice = itemRequest.getPrice();
            BigDecimal defaultPrice = product.getDefaultSalePrice();
            BigDecimal costPrice = product.getCostPrice();
            
            // 1. 校验价格非负
            if (requestedPrice == null || requestedPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new MyException("商品 [" + product.getName() + "] 销售价格必须大于0");
            }

            // 2. 校验价格是否低于成本价
            if (requestedPrice.compareTo(costPrice) < 0) {
                throw new MyException("商品 [" + product.getName() + "] 销售价格 (" + requestedPrice +
                                    ") 不能低于成本价 (" + costPrice + ")");
            }

            // 3. 设置isDefaultPrice标记
            boolean isDefaultPrice = requestedPrice.compareTo(defaultPrice) == 0;
            
            // 创建订单详情
            OrderDetail orderDetail = order.createOrderDetail(product, itemRequest);
            orderDetail.setDefaultPrice(isDefaultPrice);

            // 处理批次商品
            if (product.isBatchManaged()) {
                // 如果没有指定批次信息，使用FIFO自动分配
                if (itemRequest.getBatchDetails() == null || itemRequest.getBatchDetails().isEmpty()) {
                    List<InventoryService.BatchAllocation> allocations =
                        inventoryService.findAvailableBatchesByFifo(product, itemRequest.getQuantity());
                    
                    // 根据FIFO分配结果创建批次销售明细
                    for (InventoryService.BatchAllocation allocation : allocations) {
                        orderDetail.addBatchDetail(
                            allocation.getBatch(),
                            allocation.getQuantity(),
                            requestedPrice
                        );
                        // 扣减库存
                        inventoryService.stockOut(product, allocation.getBatch(), allocation.getQuantity());
                        // 记录库存流水 (销售出库)
                        inventoryTransactionService.recordTransactionForSales(product, allocation.getBatch(), -allocation.getQuantity(), OperationType.销售出库, order);
                    }
                } else {
                    // 如果指定了批次信息，按指定批次处理
                    for (OrderCreateRequest.BatchSaleDetail batchDetail : itemRequest.getBatchDetails()) {
                        BatchQuery batchQuery = BatchQuery.builder()
                                                          .id(batchDetail.getBatchId())
                                                          .build();
                        Batch batch = batchService.findOne(batchQuery)
                                                  .orElseThrow(() -> new MyException("批次不存在: " + batchDetail.getBatchNumber()));

                        orderDetail.addBatchDetail(batch, batchDetail.getQuantity(), requestedPrice);
                        // 扣减库存
                        inventoryService.stockOut(product, batch, batchDetail.getQuantity());
                        // 记录库存流水 (销售出库)
                        inventoryTransactionService.recordTransactionForSales(product, batch, -batchDetail.getQuantity(), OperationType.销售出库, order);
                    }
                }
            } else {
               // 非批次商品直接扣减库存
               inventoryService.stockOut(product, itemRequest.getQuantity());
               // 记录库存流水 (销售出库)
               inventoryTransactionService.recordTransactionForSales(product, null, -itemRequest.getQuantity(), OperationType.销售出库, order);
            }
        }

        // 保存订单
        orderRepository.save(order);
    }

    /**
     * 取消销售订单
     */
    @Transactional
    public void cancelOrder(Integer orderId) {
        // 1. 查找订单
        Order order = orderRepository.findById(orderId)
                                     .orElseThrow(() -> new MyException("订单不存在: " + orderId));

        // 2. 处理每个商品的入库
        for (OrderDetail orderDetail : order.getOrderDetails()) {
            Product product = orderDetail.getProduct();

            if (product.isBatchManaged()) {
                // 对于批次商品，需要处理每个批次的入库
                List<SaleBatchDetail> batchDetails = saleBatchDetailService.findByOrderDetail(orderDetail.getId());
                for (SaleBatchDetail batchDetail : batchDetails) {
                    // 批次入库
                    inventoryService.stockIn(
                            product,
                            batchDetail.getBatch(),
                            batchDetail.getQuantity()
                    );
                    // 记录库存流水 (取消销售订单)
                    inventoryTransactionService.recordTransactionForSales(product, batchDetail.getBatch(), batchDetail.getQuantity(), OperationType.取消销售订单, order);
                }
            } else {
                // 对于非批次商品，直接入库
                inventoryService.stockIn(
                        product,
                        orderDetail.getQuantity()
                );
                // 记录库存流水 (取消销售订单)
                inventoryTransactionService.recordTransactionForSales(product, null, orderDetail.getQuantity(), OperationType.取消销售订单, order);
            }
        }

        // 3. 删除订单
        orderRepository.delete(order);
    }


}
