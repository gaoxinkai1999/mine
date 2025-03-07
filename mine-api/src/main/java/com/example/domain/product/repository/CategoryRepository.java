package com.example.domain.product.repository;

import com.example.domain.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    /**
     * 查找当前最大sort
     * @return
     */
    @Query("SELECT MAX(c.sort) FROM Category c")
    int findMaxSort();
}