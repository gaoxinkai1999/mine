package com.example.domain.inventory.repository;

import com.example.domain.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {


    /**
     * 查询商品指定批次的库存记录
     */
    Optional<Inventory> findByProductIdAndBatchId(Integer productId, Integer batchId);

    /**
     * 查询商品非批次库存记录
     */
    Optional<Inventory> findByProductIdAndBatchIdIsNull(Integer productId);


}