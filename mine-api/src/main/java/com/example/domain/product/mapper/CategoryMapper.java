package com.example.domain.product.mapper;

import com.example.domain.product.dto.CategoryRequestDto;
import com.example.domain.product.dto.CategoryUpdateDto;
import com.example.domain.product.entity.Category;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CategoryMapper {
    Category toEntity(CategoryRequestDto categoryRequestDto);

    CategoryRequestDto toDto(Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(CategoryRequestDto categoryRequestDto, @MappingTarget Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(CategoryUpdateDto categoryUpdateDto, @MappingTarget Category category);
}