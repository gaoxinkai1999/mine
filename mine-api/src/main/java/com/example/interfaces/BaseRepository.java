package com.example.interfaces;

import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Optional;

/**
 * 基础Repository接口
 * 
 * @param <T> 实体类型
 * @param <Q> 查询条件类型
 */
public interface BaseRepository<T, Q extends BaseQuery> {

    /**
     * 查询单个结果
     * @param query 查询条件
     * @return Optional包装的查询结果
     */
    default Optional<T> findOne(Q query) {
        return Optional.ofNullable(buildBaseQuery(query).fetchOne());
    }

    /**
     * 查询列表结果
     * @param query 查询条件
     * @return 查询结果列表
     */
    default List<T> findList(Q query) {
        JPAQuery<T> jpaQuery = buildBaseQuery(query);
        return jpaQuery.fetch();
    }

    /**
     * 分页查询
     * @param query 查询条件
     * @param pageable 分页参数
     * @return 分页结果
     */
    default Slice<T> findPage(Q query, Pageable pageable) {
        JPAQuery<T> jpaQuery = buildBaseQuery(query);
        
        // 执行分页查询
        List<T> content = jpaQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content.removeLast();
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    /**
     * 构建基础查询
     * 该方法首先使用buildConditionQuery构建基本条件查询，然后使用buildRelationship加载关联对象
     * @param query 查询条件
     * @return JPAQuery对象
     */
    default JPAQuery<T> buildBaseQuery(Q query) {
        JPAQuery<T> jpaQuery = buildConditionQuery(query);
        buildRelationship(query, jpaQuery);
        return jpaQuery;
    }
    
    /**
     * 构建基本条件查询
     * 该方法只构建基本的查询条件，不加载关联对象
     * @param query 查询条件
     * @return JPAQuery对象
     */
    JPAQuery<T> buildConditionQuery(Q query);
    
    /**
     * 加载关联对象
     * 该方法根据查询条件加载关联对象
     * @param query 查询条件
     * @param jpaQuery 已构建的基本查询
     */
    void buildRelationship(Q query, JPAQuery<T> jpaQuery);
}
