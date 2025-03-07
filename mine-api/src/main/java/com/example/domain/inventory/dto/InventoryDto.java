package com.example.domain.inventory.dto;

import com.example.domain.batch.entity.Batch;
import com.example.domain.product.entity.Product;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDto {
    private Integer id;
    private Integer quantity;

    private ProductDto product;
    private BatchDto batch;


    /**
     * DTO for {@link Product}
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductDto implements Serializable {
        private int id;
        private String name;
        private BigDecimal costPrice;
        private BigDecimal defaultSalePrice;
        private boolean isDel;
        private int sort;
        private boolean isBatchManaged = false;
    }

    /**
     * DTO for {@link Batch}
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BatchDto implements Serializable {
        private Integer id;
        private String batchNumber;
        private LocalDate productionDate;
        private LocalDate expirationDate;
        private BigDecimal costPrice;
        private Boolean status = true;
        private String remark;
        private LocalDateTime createdTime = LocalDateTime.now();
    }
}
