package com.example.domain.purchase.mapper;

import com.example.domain.purchase.dto.PurchaseDetailDto;
import com.example.domain.purchase.dto.PurchaseDto;
import com.example.domain.purchase.entity.Purchase;
import com.example.domain.purchase.entity.PurchaseDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 采购单映射器
 * 处理实体和DTO之间的转换
 */
@Mapper(componentModel = "spring")
public interface PurchaseMapper {

    PurchaseMapper INSTANCE = Mappers.getMapper(PurchaseMapper.class);
    
    /**
     * 将采购单实体转换为DTO
     * @param purchase 采购单实体
     * @return 采购单DTO
     */
    PurchaseDto toPurchaseDTO(Purchase purchase);
    
    /**
     * 将采购单实体列表转换为DTO列表
     * @param purchases 采购单实体列表
     * @return 采购单DTO列表
     */
    List<PurchaseDto> toPurchaseDTOList(List<Purchase> purchases);
    
    /**
     * 将采购单明细实体转换为DTO
     * @param purchaseDetail 采购单明细实体
     * @return 采购单明细DTO
     */
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "batch.id", target = "batchId")
    @Mapping(source = "product.batchManaged", target = "batchManaged")
    PurchaseDetailDto toPurchaseDetailDTO(PurchaseDetail purchaseDetail);
    
    /**
     * 将采购单明细实体列表转换为DTO列表
     * @param purchaseDetails 采购单明细实体列表
     * @return 采购单明细DTO列表
     */
    List<PurchaseDetailDto> toPurchaseDetailDTOList(List<PurchaseDetail> purchaseDetails);
} 