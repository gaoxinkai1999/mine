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
public class ShopRequestDto implements Serializable {
    private Integer id;
    private String name;
    private String location;
    private Character pinyin;
    private Boolean del = false;
    private BigDecimal arrears;
    private BigDecimal longitude;
    private BigDecimal latitude;
}