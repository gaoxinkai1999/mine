package com.example.domain.order.service;

import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Transactional(readOnly = true)
public abstract class BaseService<E, ID, Q extends EntityPathBase<E>> {

    protected final JPAQueryFactory queryFactory;
    protected final Q qEntity;
    protected final Class<E> entityClass;

    public BaseService(JPAQueryFactory queryFactory, Q qEntity, Class<E> entityClass) {
        this.queryFactory = queryFactory;
        this.qEntity = qEntity;
        this.entityClass = entityClass;
    }

    /**
     * 动态查询列表
     */
    public List<E> findAll(Function<Q, Predicate> predicateFunction, Consumer<JPAQuery<E>> queryCustomizer) {
        Predicate predicate = predicateFunction.apply(qEntity);
        JPAQuery<E> query = queryFactory.selectFrom(qEntity)
                                        .where(predicate);
        if (queryCustomizer != null) queryCustomizer.accept(query);
        return query.fetch();
    }

    /**
     * 查询单个结果（唯一性校验）
     */
    public Optional<E> findOne(Function<Q, Predicate> predicateFunction, Consumer<JPAQuery<E>> queryCustomizer) {
        Predicate predicate = predicateFunction.apply(qEntity);
        JPAQuery<E> query = queryFactory.selectFrom(qEntity)
                                        .where(predicate);
        if (queryCustomizer != null) queryCustomizer.accept(query);
        try {
            return Optional.ofNullable(query.fetchOne());
        } catch (NonUniqueResultException e) {
            throw new IllegalStateException("Multiple results found for query expecting single result", e);
        }
    }

    /**
     * 分页查询
     */
    public Page<E> findPage(
            Function<Q, Predicate> predicateFunction,
            Pageable pageable, // 保留原始分页参数
            OrderSpecifier<?>[] orderSpecifiers, // 新增排序参数
            Consumer<JPAQuery<E>> queryCustomizer
    ) {
        Predicate predicate = predicateFunction.apply(qEntity);

        // COUNT查询（不需要排序）
        long total = queryFactory.selectFrom(qEntity)
                                 .where(predicate)
                                 .fetchCount();

        // DATA查询
        JPAQuery<E> query = queryFactory.selectFrom(qEntity)
                                        .where(predicate);
        if (queryCustomizer != null) queryCustomizer.accept(query);

        // 优先使用OrderSpecifier排序
        if (orderSpecifiers != null && orderSpecifiers.length > 0) {
            query.orderBy(orderSpecifiers);
        }

        List<E> content = query.offset(pageable.getOffset())
                               .limit(pageable.getPageSize())
                               .fetch();

        return new PageImpl<>(content, pageable, total);
    }


}
