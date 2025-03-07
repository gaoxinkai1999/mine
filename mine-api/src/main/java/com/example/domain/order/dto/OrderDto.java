package com.example.domain.order.dto;

import com.example.domain.order.entity.Order;
import com.example.domain.order.entity.OrderDetail;
import com.example.domain.product.entity.Product;
import com.example.domain.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for {@link Order}
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDto implements Serializable {
    private int id;
    private ShopDto shop;
    private LocalDateTime createTime = LocalDateTime.now();
    private BigDecimal totalSalesAmount;
    private BigDecimal totalProfit;
    private List<OrderDetailDto> orderDetails = new ArrayList<>();

    /**
     * DTO for {@link Shop}
     */
    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopDto implements Serializable {
        private int id;
        private String name;
        private String location;
    }

    /**
     * DTO for {@link OrderDetail}
     */
    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderDetailDto implements Serializable {
        private Integer id;
        private ProductDto product;
        private BigDecimal totalSalesAmount;
        private BigDecimal totalProfit;
        private Integer quantity;

        private BigDecimal costPrice;
        private BigDecimal salePrice;
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
            private int sort;
        }
    }
}