package com.example.domain.order.service;

import com.example.domain.order.entity.OrderDetail;
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
     * 构建基础查询
     * 根据查询条件构建QueryDSL查询对象
     *
     * @param query 查询条件
     * @return 构建好的查询对象
     */
    @Override
    public JPAQuery<OrderDetail> buildBaseQuery(OrderDetailQuery query) {
        QOrderDetail orderDetail = QOrderDetail.orderDetail;
        QProduct product = QProduct.product;
        QShop shop = QShop.shop;
        
        // 创建基础查询
        JPAQuery<OrderDetail> jpaQuery = queryFactory.selectFrom(orderDetail)
                .distinct();
        
        // 处理关联
        if (query.getIncludes().contains(OrderDetailQuery.Include.PRODUCT)) {
            jpaQuery.leftJoin(orderDetail.product, product).fetchJoin();
        }
        
        if (query.getIncludes().contains(OrderDetailQuery.Include.ORDER)) {
            jpaQuery.leftJoin(orderDetail.order).fetchJoin();
        }
        
        if (query.getIncludes().contains(OrderDetailQuery.Include.SHOP)) {
            jpaQuery.leftJoin(orderDetail.order.shop, shop).fetchJoin();
        }
        
        // 构建查询条件
        BooleanBuilder where = new BooleanBuilder();
        
        // 根据ID查询
        if (query.getId() != null) {
            where.and(orderDetail.id.eq(query.getId()));
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
        
        // 应用查询条件并按订单创建时间降序排序
        return jpaQuery.where(where)
                .orderBy(orderDetail.order.createTime.desc());
    }
} 