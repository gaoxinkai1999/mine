package com.example.domain.returnOrder.service;


import com.example.domain.inventory.service.InventoryService;
import com.example.domain.product.entity.Product;
import com.example.domain.product.service.ProductService;
import com.example.domain.returnOrder.dto.ReturnOrderDto;
import com.example.domain.returnOrder.dto.ReturnOrderRequest;
import com.example.domain.returnOrder.entity.QReturnOrder;
import com.example.domain.returnOrder.entity.QReturnOrderDetail;
import com.example.domain.returnOrder.entity.ReturnOrder;
import com.example.domain.returnOrder.entity.ReturnOrderDetail;
import com.example.domain.returnOrder.mapper.ReturnOrderMapper;
import com.example.domain.returnOrder.repository.ReturnOrderDetailRepository;
import com.example.domain.returnOrder.repository.ReturnOrderRepository;
import com.example.domain.shop.entity.QShop;
import com.example.domain.shop.entity.Shop;
import com.example.domain.shop.service.ShopService;
import com.example.exception.MyException;
import com.example.interfaces.BaseRepository;
import com.example.query.ProductQuery;
import com.example.query.ReturnOrderQuery;
import com.example.query.ShopQuery;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 退货订单服务
 * 处理退货订单的增删改查及相关业务逻辑
 */
@Service
@Slf4j
public class ReturnOrderService implements BaseRepository<ReturnOrder, ReturnOrderQuery> {

    @Autowired
    private ReturnOrderRepository returnOrderRepository;

    @Autowired
    private ReturnOrderDetailRepository returnOrderDetailRepository;

    @Autowired
    private ShopService shopService;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ReturnOrderMapper returnOrderMapper;

    @Autowired
    private JPAQueryFactory queryFactory;

    /**
     * 构建基本条件查询
     * 根据传入的查询参数，构建一个基本的JPAQuery对象，只包含条件查询
     *
     * @param query 查询参数
     * @return 查询对象
     */
    @Override
    public JPAQuery<ReturnOrder> buildConditionQuery(ReturnOrderQuery query) {
        QReturnOrder qReturnOrder = QReturnOrder.returnOrder;
        QReturnOrderDetail qReturnOrderDetail = QReturnOrderDetail.returnOrderDetail;

        // 初始化查询对象
        JPAQuery<ReturnOrder> jpaQuery = queryFactory.selectFrom(qReturnOrder)
                .distinct();

        // 处理查询条件
        BooleanBuilder where = new BooleanBuilder();

        if (query.getId() != null) {
            where.and(qReturnOrder.id.eq(query.getId()));
        }
        if (query.getIds() != null) {
            where.and(qReturnOrder.id.in(query.getIds()));
        }

        if (query.getShopId() != null) {
            where.and(qReturnOrder.shop.id.eq(query.getShopId()));
        }

        if (query.getStartTime() != null && query.getEndTime() != null) {
            Instant startInstant = query.getStartTime().atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            Instant endInstant = query.getEndTime().plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            where.and(qReturnOrder.createTime.between(startInstant, endInstant));
        }

        if (query.getReturnType() != null) {
            // 这里需要连接详情表来按照退货类型过滤
            where.and(qReturnOrderDetail.type.eq(query.getReturnType()));
        }

        return jpaQuery.where(where).orderBy(qReturnOrder.createTime.desc());
    }

    /**
     * 构建关联加载
     * 根据查询条件加载关联对象
     *
     * @param query 查询条件
     * @param jpaQuery 已构建的基本查询对象
     */
    @Override
    public void buildRelationship(ReturnOrderQuery query, JPAQuery<ReturnOrder> jpaQuery) {
        QReturnOrder qReturnOrder = QReturnOrder.returnOrder;
        QReturnOrderDetail qReturnOrderDetail = QReturnOrderDetail.returnOrderDetail;
        QShop qShop = QShop.shop;

        // 处理关联查询
        if (query.getIncludes().contains(ReturnOrderQuery.Include.SHOP)) {
            jpaQuery.leftJoin(qReturnOrder.shop, qShop).fetchJoin();
        }

        if (query.getIncludes().contains(ReturnOrderQuery.Include.DETAILS)) {
            jpaQuery.leftJoin(qReturnOrder.returnOrderDetails, qReturnOrderDetail).fetchJoin();
        }
    }

    /**
     * 创建退货订单
     *
     * @param request 退货订单请求
     * @return 创建的退货订单DTO
     */
    @Transactional
    public void createReturnOrder(ReturnOrderRequest request) {
        // 1. 查找商店
        Shop shop = shopService.findOne(ShopQuery.builder()
                                                 .id(request.getShopId())
                                                 .build())
                .orElseThrow(() -> new MyException("商店不存在: " + request.getShopId()));

        // 2. 创建退货订单
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.setShop(shop);
        returnOrder.setCreateTime(Instant.now());
        
        // 3. 计算退货总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        // 4. 保存退货订单
        ReturnOrder savedReturnOrder = returnOrderRepository.save(returnOrder);
        
        // 5. 创建退货订单详情
        for (ReturnOrderRequest.ReturnOrderDetailRequest detailRequest : request.getDetails()) {
            // 验证：如果是退货退款类型，必须有退货数量
            if (detailRequest.getType() == com.example.domain.returnOrder.entity.ReturnType.退货退款) {
                if (detailRequest.getQuantity() == null || detailRequest.getQuantity() <= 0) {
                    throw new MyException("退货退款类型必须指定退货数量");
                }
            }
            
            // 查找商品
            Product product = productService.findOne(ProductQuery.builder()
                                                                 .id(detailRequest.getProductId())
                                                                 .build())
                    .orElseThrow(() -> new MyException("商品不存在: " + detailRequest.getProductId()));
            
            // 创建退货订单详情
            ReturnOrderDetail detail = new ReturnOrderDetail();
            detail.setReturnOrder(savedReturnOrder);
            detail.setProduct(product);
            detail.setAmount(detailRequest.getAmount());
            detail.setType(detailRequest.getType());
            detail.setQuantity(detailRequest.getQuantity());
            returnOrderDetailRepository.save(detail);
            
            // 累计退货金额
            totalAmount = totalAmount.add(detailRequest.getAmount());
            
            // 处理库存
            if (detailRequest.getType() == com.example.domain.returnOrder.entity.ReturnType.退货退款) {
                // 退货入库，使用实际的退货数量
                inventoryService.stockIn(product, detailRequest.getQuantity());
                // 这里可以记录库存操作，如果有需要
                // inventoryService.recordTransaction(product, null, detailRequest.getQuantity(), OperationType.退货入库);
            }
        }
        
        // 6. 更新退货订单总金额
        savedReturnOrder.setAmount(totalAmount);
        returnOrderRepository.save(savedReturnOrder);
    }

    /**
     * 删除退货订单（硬删除）
     *
     * @param returnOrderId 退货订单ID
     */
    @Transactional
    public void deleteReturnOrder(Integer returnOrderId) {
        // 1. 查找退货订单
        ReturnOrder returnOrder = returnOrderRepository.findById(returnOrderId)
                .orElseThrow(() -> new MyException("退货订单不存在: " + returnOrderId));
        
        // 处理退货退款类型的库存回退
        List<ReturnOrderDetail> details = returnOrderDetailRepository.findByReturnOrderId(returnOrderId);
        for (ReturnOrderDetail detail : details) {
            // 如果是退货退款类型且有退货数量，需要回退库存
            if (detail.getType().name().equals("退货退款") && 
                detail.getQuantity() != null && detail.getQuantity() > 0) {
                // 商品退货入库时增加了库存，删除时应减少库存
                Product product = detail.getProduct();
                // 减少库存（回退之前的入库操作）
                inventoryService.stockOut(product, detail.getQuantity());
                log.info("商品库存已回退: 商品={}, 数量={}", product.getName(), detail.getQuantity());
            }
        }
        
        // 明确删除所有关联的退货订单详情
        for (ReturnOrderDetail detail : details) {
            returnOrderDetailRepository.delete(detail);
        }
        log.info("已删除退货订单详情记录: {}", details.size());
        
        // 2. 删除退货订单
        returnOrderRepository.delete(returnOrder);
        
        log.info("退货订单已删除: {}", returnOrderId);
    }
    
    /**
     * 根据ID查询退货订单
     *
     * @param returnOrderId 退货订单ID
     * @return 退货订单DTO
     */
    public ReturnOrderDto getReturnOrderById(Integer returnOrderId) {
        ReturnOrder returnOrder = findOne(ReturnOrderQuery.builder()
                .id(returnOrderId)
                .includes(ReturnOrderQuery.Include.FULL)
                .build())
                .orElseThrow(() -> new MyException("退货订单不存在: " + returnOrderId));
        
        return returnOrderMapper.toReturnOrderDTO(returnOrder);
    }
    
    /**
     * 查询退货订单列表
     *
     * @param shopId    商店ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @return 退货订单DTO列表
     */
    public List<ReturnOrderDto> getReturnOrders(Integer shopId, LocalDate startDate, LocalDate endDate) {
        ReturnOrderQuery.ReturnOrderQueryBuilder queryBuilder = ReturnOrderQuery.builder()
                .includes(ReturnOrderQuery.Include.FULL);
        
        if (shopId != null) {
            queryBuilder.shopId(shopId);
        }
        
        if (startDate != null && endDate != null) {
            queryBuilder.startTime(startDate);
            queryBuilder.endTime(endDate);
        }
        
        List<ReturnOrder> returnOrders = findList(queryBuilder.build());
        return returnOrderMapper.toReturnOrderDTOList(returnOrders);
    }
} 