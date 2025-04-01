package com.example.domain.shop.dto;

import com.example.domain.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;

/**
 * DTO for {@link Shop}
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopSimpleDto implements Serializable {
    private int id;
    private String name;
    private String location;
    private char pinyin;

}