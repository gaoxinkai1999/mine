package com.example.domain.shop.dto;

import com.example.domain.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link Shop}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopArrearsDto implements Serializable {
    private int id;
    private String name;
    private String location;
    private BigDecimal arrears;
}