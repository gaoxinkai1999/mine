package com.example.domain.product.dto;

import lombok.Data;

@Data
public class CategoryUpdateDto {
    private Integer id;
    private String name;
    private Integer sort;
    private Boolean del;
} 