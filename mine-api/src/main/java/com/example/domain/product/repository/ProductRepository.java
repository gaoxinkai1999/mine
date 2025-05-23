package com.example.domain.product.repository;

import com.example.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {



    /**
     * 查找当前最大sort
     * @return
     */
    @Query("SELECT MAX(p.sort) FROM Product p")
    int findMaxSort();

    /**
     * 通过 id 列表查询对应的 name
     *
     * @param ids id 列表
     * @return 对应的 name 列表
     */
    @Query("SELECT p.name FROM Product p WHERE p.id IN :ids")
    List<String> findNamesByIds(@Param("ids") int[] ids);

    /**
     * 根据商品ID查询最早销售日期
     *
     * @param productId 商品ID
     * @return 商品最早销售日期
     */
    @Query("SELECT MIN(o.createTime) FROM Order o JOIN o.orderDetails od WHERE od.product.id = :productId")
    LocalDate findEarliestSaleDateByProductId(@Param("productId") Integer productId);

    /**
     * 检查商品名称是否已存在
     * @param name 商品名称
     * @return 是否存在
     */
    boolean existsByName(String name);
/**
     * 查询所有设置了临期阈值且需要批次管理的商品
     * @return 商品列表
     */
    List<Product> findByExpiryMonitoringThresholdDaysIsNotNullAndBatchManagedIsTrue();
}
