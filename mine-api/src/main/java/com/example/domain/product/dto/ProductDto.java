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
 * 基础商品信息dto，包含库存
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDto implements Serializable {
    private int id;
    private String name;
    private BigDecimal costPrice;
    private BigDecimal defaultSalePrice;
    private boolean del;
    private Integer categoryId;
    private int sort;
    private boolean batchManaged;
    private ProductStockDTO productStockDTO;


}