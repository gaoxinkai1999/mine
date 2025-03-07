package com.example.domain.price.repository;

import com.example.domain.price.entity.PriceRuleDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRuleDetailRepository extends JpaRepository<PriceRuleDetail, Integer> {
}