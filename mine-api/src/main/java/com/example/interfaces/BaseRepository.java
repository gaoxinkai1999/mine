package com.example.interfaces;

import com.querydsl.jpa.impl.JPAQuery;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        try {
            // 1. 构建基础查询并执行分页
            JPAQuery<T> jpaQuery = buildConditionQuery(query);
            List<T> content = jpaQuery
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize() + 1)
                    .fetch();

            // 2. 处理分页
            boolean hasNext = content.size() > pageable.getPageSize();
            if (hasNext) {
                content.removeLast();
            }

            // 3. 如果需要加载关联实体，重新查询完整数据
            if (query.getIncludes() != null && !query.getIncludes().isEmpty()) {
                // 提取实体ID列表
                List<Integer> ids = content.stream()
                        .map(entity -> {
                            try {
                                return (Integer) entity.getClass().getMethod("getId").invoke(entity);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to get entity ID", e);
                            }
                        })
                        .collect(Collectors.toList());

                // 使用现有查询对象的复制来创建新查询
                // 只保留ID列表和includes设置
                Q enrichQuery;
                try {
                    // 尝试获取查询类的Builder
                    Object builder = query.getClass().getMethod("builder").invoke(null);

                    // 设置IDs
                    builder.getClass().getMethod("ids", Integer[].class)
                            .invoke(builder, new Object[]{ids.toArray(new Integer[0])});

                    // 设置includes
                    builder.getClass().getMethod("includes", Set.class)
                            .invoke(builder, query.getIncludes());

                    // 构建查询对象
                    enrichQuery = (Q) builder.getClass().getMethod("build").invoke(builder);
                    System.out.println("enrichQuery: " + enrichQuery.toString());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create query object using Builder pattern", e);
                }

                // 重新查询完整数据
                content = findList(enrichQuery);
            }

            return new SliceImpl<>(content, pageable, hasNext);
        } catch (Exception e) {
            throw new RuntimeException("Error in findPage: " + e.getMessage(), e);
        }
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
