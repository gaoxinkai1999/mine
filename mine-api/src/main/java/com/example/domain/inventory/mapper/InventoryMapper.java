package com.example.domain.inventory.mapper;

import com.example.domain.batch.repository.BatchRepository;
import com.example.domain.inventory.dto.InventoryUpdateDto;
import com.example.domain.inventory.entity.Inventory;
import com.example.domain.product.repository.ProductRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class InventoryMapper {
    
    @Autowired
    protected ProductRepository productRepository;
    
    @Autowired
    protected BatchRepository batchRepository;


    @Mapping(target = "product", ignore = true)
    @Mapping(target = "batch", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract Inventory partialUpdate(InventoryUpdateDto inventoryUpdateDto, @MappingTarget Inventory inventory);

    @AfterMapping
    protected void afterPartialUpdate(InventoryUpdateDto inventoryUpdateDto, @MappingTarget Inventory inventory) {
        if (inventoryUpdateDto.getProductId() != null) {
            inventory.setProduct(productRepository.getReferenceById(inventoryUpdateDto.getProductId()));
        }
        if (inventoryUpdateDto.getBatchId() != null) {
            inventory.setBatch(batchRepository.getReferenceById(inventoryUpdateDto.getBatchId()));
        }
    }



}