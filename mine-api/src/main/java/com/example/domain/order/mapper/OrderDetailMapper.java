package com.example.domain.order.mapper;

import com.example.domain.order.dto.ProductOrderItemDto;
import com.example.domain.order.entity.OrderDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 订单详情映射器
 * 用于实体和DTO之间的转换
 */
@Mapper(componentModel = "spring")
public interface OrderDetailMapper {
    
    /**
     * 将订单详情实体转换为产品订单项DTO
     *
     * @param orderDetail 订单详情实体
     * @return 产品订单项DTO
     */
    @Mapping(source = "id", target = "orderDetailId")
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.createTime", target = "orderTime")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "order.shop.id", target = "shopId")
    @Mapping(source = "order.shop.name", target = "shopName")
    ProductOrderItemDto toProductOrderItemDto(OrderDetail orderDetail);
    
    /**
     * 将订单详情实体列表转换为产品订单项DTO列表
     *
     * @param orderDetails 订单详情实体列表
     * @return 产品订单项DTO列表
     */
    List<ProductOrderItemDto> toProductOrderItemDtoList(List<OrderDetail> orderDetails);
} 