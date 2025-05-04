package com.example.domain.order.repository;

import com.example.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List; // 导入 List
import java.util.Optional;

/**
 * Order 数据访问仓库
 */
public interface OrderRepository extends JpaRepository<Order, Integer>  {

    /**
     * 查询指定商家和商品的最新订单创建时间
     * @param shopId 商家ID
     * @param productId 商品ID
     * @return 最新的订单创建时间 (LocalDateTime), 如果没有则返回 Optional.empty()
     */
    @Query("SELECT MAX(o.createTime) FROM Order o JOIN o.orderDetails od WHERE o.shop.id = :shopId AND od.product.id = :productId")
    Optional<LocalDateTime> findLatestOrderCreateTimeByShopAndProduct(@Param("shopId") Integer shopId, @Param("productId") Integer productId);


    /**
     * 查找指定商家包含批次管理商品的最新订单创建时间
     * @param shopId 商家ID
     * @return 最新的订单创建时间 (LocalDateTime), 如果没有则返回 Optional.empty()
     */
    @Query("SELECT MAX(o.createTime) FROM Order o JOIN o.orderDetails od JOIN od.product p WHERE o.shop.id = :shopId AND p.batchManaged = true")
    Optional<LocalDateTime> findLatestOrderTimeWithBatchManagedProductByShop(@Param("shopId") Integer shopId);

    /**
     * 根据商家ID和精确的创建时间查找订单ID列表
     * @param shopId 商家ID
     * @param createTime 订单创建时间
     * @return 订单ID列表
     */
    @Query("SELECT o.id FROM Order o WHERE o.shop.id = :shopId AND o.createTime = :createTime")
    List<Integer> findOrderIdsByShopIdAndCreateTime(@Param("shopId") Integer shopId, @Param("createTime") LocalDateTime createTime);

}
