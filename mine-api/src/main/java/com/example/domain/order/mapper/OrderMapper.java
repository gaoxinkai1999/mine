package com.example.domain.order.mapper;

import com.example.domain.order.dto.OrderDto;
import com.example.domain.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    // Order -> OrderDTO
    OrderDto toOrderDTO(Order order);

    // List<Order> -> List<OrderDTO>
    List<OrderDto> toOrderDTOList(List<Order> orders);



}
