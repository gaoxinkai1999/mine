package com.example.domain.batch.mapper;

import com.example.domain.batch.dto.BatchUpdateDto;
import com.example.domain.batch.entity.Batch;
import com.example.domain.product.repository.ProductRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class BatchMapper {
    
    @Autowired
    protected ProductRepository productRepository;

    @Mapping(target = "product", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract Batch partialUpdate(BatchUpdateDto batchUpdateDto, @MappingTarget Batch batch);

    @AfterMapping
    protected void afterPartialUpdate(BatchUpdateDto batchUpdateDto, @MappingTarget Batch batch) {
        if (batchUpdateDto.getProductId() != null) {
            batch.setProduct(productRepository.getReferenceById(batchUpdateDto.getProductId()));
        }
    }
} 