package com.example.domain.product.dto;

import com.example.domain.product.entity.Product;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link Product}
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductUpdateDto implements Serializable {
    private Integer id;
    private String name;
    private BigDecimal costPrice;
    private BigDecimal defaultSalePrice;
    private Boolean isDel;
    private Integer sort;
    private Integer categoryId;
    private Boolean isBatchManaged;
}