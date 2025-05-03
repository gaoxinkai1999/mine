package com.example.domain.batch.mapper;

import com.example.domain.batch.dto.BatchDto; // 导入 BatchDto
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

    /**
     * 将 Batch 实体转换为 BatchDto
     * @param batch Batch 实体
     * @return BatchDto
     */
    // MapStruct 会自动映射同名字段 (id, batchNumber, productionDate, expirationDate)
    public abstract BatchDto toDto(Batch batch);

    // Removed afterPartialUpdate method as productId is no longer part of the DTO
    // and updating batch info should not change the associated product via the mapper.
}