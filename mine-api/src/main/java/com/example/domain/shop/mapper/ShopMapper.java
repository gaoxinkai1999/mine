package com.example.domain.shop.mapper;

import com.example.domain.shop.dto.*;
import com.example.domain.shop.entity.Shop;
import com.example.exception.MyException;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class ShopMapper {


    public abstract ShopSimpleDto toShopSimpleDto(Shop shop);

    // 集合转换（MapStruct 自动实现）
    public abstract List<ShopSimpleDto> toShopSimpleDtoList(List<Shop> shops);


    public abstract ShopLocationDto toShopLocationDto(Shop shop);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void partialUpdate(ShopRequestDto shopRequestDto, @MappingTarget Shop shop);



    public abstract ShopDto toShopDto(Shop shop);


    public abstract ShopArrearsDto toShopArrearsDto(Shop shop);


    public abstract Shop toEntity(ShopDto shopDto);
}