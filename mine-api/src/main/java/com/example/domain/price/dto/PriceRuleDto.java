package com.example.domain.price.dto;

import com.example.domain.price.entity.PriceRule;
import com.example.domain.price.entity.PriceRuleDetail;
import com.example.domain.product.entity.Product;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for {@link PriceRule}
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceRuleDto implements Serializable {
    private Integer id;
    private String name;
    private boolean isDie;
    private List<PriceRuleDetailDto> priceRuleDetails;
    private String color;

    /**
     * DTO for {@link PriceRuleDetail}
     */
    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceRuleDetailDto implements Serializable {
        private Integer id;
        private ProductDto product;
        private BigDecimal price;
        private boolean isDefaultPrice;

        /**
         * DTO for {@link Product}
         */
        @Getter
        @Setter
        @ToString
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ProductDto implements Serializable {
            private int id;
            private String name;
            private BigDecimal costPrice;
            private BigDecimal defaultSalePrice;
            private BigDecimal discountedPrice;
            private int inventory;
            private boolean isDel;
            private int sort;
        }
    }
}