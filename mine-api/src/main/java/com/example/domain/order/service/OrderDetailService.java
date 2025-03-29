package com.example.domain.order.service;

import com.example.domain.order.entity.OrderDetail;
import com.example.domain.order.entity.QOrder;
import com.example.domain.order.entity.QOrderDetail;
import com.example.domain.order.repository.OrderDetailRepository;
import com.example.domain.product.entity.QProduct;
import com.example.domain.shop.entity.QShop;
import com.example.interfaces.BaseRepository;
import com.example.query.OrderDetailQuery;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 订单详情服务
 * 处理订单详情的查询和管理
 */
@Service
public class OrderDetailService implements BaseRepository<OrderDetail, OrderDetailQuery> {
    
    @Autowired
    private OrderDetailRepository orderDetailRepository;
    
    @Autowired
    private JPAQueryFactory queryFactory;
    
    /**
     * 构建基本条件查询
     * 根据查询条件构建基本的QueryDSL查询对象，只包含查询条件
     *
     * @param query 查询条件
     * @return 构建好的基本查询对象
     */
    @Override
    public JPAQuery<OrderDetail> buildConditionQuery(OrderDetailQuery query) {
        QOrderDetail orderDetail = QOrderDetail.orderDetail;
        
        // 创建基础查询
        JPAQuery<OrderDetail> jpaQuery = queryFactory.selectFrom(orderDetail)
                .distinct();
        
        // 构建查询条件
        BooleanBuilder where = new BooleanBuilder();
        
        // 根据ID查询
        if (query.getId() != null) {
            where.and(orderDetail.id.eq(query.getId()));
        }

        // 根据订单ID列表查询
        if (query.getIds() != null) {
            where.and(orderDetail.id.in(query.getIds()));
        }
        
        // 根据产品ID查询
        if (query.getProductId() != null) {
            where.and(orderDetail.product.id.eq(query.getProductId()));
        }
        
        // 根据订单ID查询
        if (query.getOrderId() != null) {
            where.and(orderDetail.order.id.eq(query.getOrderId()));
        }
        
        // 根据时间范围查询
        if (query.getStartTime() != null && query.getEndTime() != null) {
            where.and(orderDetail.order.createTime.between(
                    query.getStartTime().atStartOfDay(),
                    query.getEndTime().atTime(23, 59, 59)
            ));
        }
        
        // 应用查询条件
        return jpaQuery.where(where);
    }
    
    /**
     * 构建关联加载
     * 根据查询条件加载关联对象
     *
     * @param query 查询条件
     * @param jpaQuery 已构建的基本查询对象
     */
    @Override
    public void buildRelationship(OrderDetailQuery query, JPAQuery<OrderDetail> jpaQuery) {
        QOrderDetail orderDetail = QOrderDetail.orderDetail;
        QProduct product = QProduct.product;
        QShop shop = QShop.shop;
        QOrder order = QOrder.order;
        // 处理关联
        if (query.getIncludes().contains(OrderDetailQuery.Include.PRODUCT)) {
            jpaQuery.leftJoin(orderDetail.product, product).fetchJoin();
        }

        // 先检查是否需要加载ORDER或SHOP
        boolean loadOrder = query.getIncludes().contains(OrderDetailQuery.Include.ORDER) ||
                          query.getIncludes().contains(OrderDetailQuery.Include.SHOP);

        // 如果需要加载ORDER或SHOP，则加载ORDER
        if (loadOrder) {
            // 使用别名 'order' 连接 Order
            jpaQuery.leftJoin(orderDetail.order, order).fetchJoin();

            // 如果需要加载SHOP，则在ORDER的基础上加载SHOP
            if (query.getIncludes().contains(OrderDetailQuery.Include.SHOP)) {
                 // 从别名 'order' 连接 Shop
                jpaQuery.leftJoin(order.shop, shop).fetchJoin();
            }
        }

        // 在建立关联后添加排序（如果需要）
        // 注意：这里假设只要加载了关联，就需要按订单时间排序
        // 如果排序是可选的，需要更复杂的逻辑来处理 Pageable 中的 Sort 信息
        if (loadOrder) { // 或者根据其他逻辑判断是否需要排序
             jpaQuery.orderBy(order.createTime.desc()); // 使用别名 'order'
        } else {
            // 如果没有加载 order，但仍然需要排序，需要确保 order 被连接
            // 或者提供一个默认排序
             jpaQuery.orderBy(orderDetail.id.desc()); // 示例：默认按详情ID排序
        }
    }
} 