package com.example.domain.product.mapper;

import com.example.domain.product.dto.ProductDto;
import com.example.domain.product.dto.ProductUpdateDto;
import com.example.domain.product.entity.Product;
import com.example.domain.product.repository.CategoryRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class ProductMapper {
    @Autowired
    protected CategoryRepository categoryRepository;

    @Mapping(source = "category.id", target = "categoryId")
    public abstract ProductDto toProductDto(Product product);




    @Mapping(target = "category", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract Product partialUpdate(ProductUpdateDto productUpdateDto, @MappingTarget Product product);

    @AfterMapping
    protected void afterPartialUpdate(ProductUpdateDto productUpdateDto, @MappingTarget Product product) {
        if (productUpdateDto.getCategoryId() != null) {
            product.setCategory(categoryRepository.getReferenceById(productUpdateDto.getCategoryId()));
        }
    }



}
