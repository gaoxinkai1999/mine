package com.example.domain.returnOrder.mapper;

import com.example.domain.returnOrder.dto.ReturnOrderDto;
import com.example.domain.returnOrder.dto.ReturnOrderDetailDto;
import com.example.domain.returnOrder.entity.ReturnOrder;
import com.example.domain.returnOrder.entity.ReturnOrderDetail;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 退货订单映射器
 * 处理实体和DTO之间的转换
 */
@Mapper(componentModel = "spring")
public interface ReturnOrderMapper {

    ReturnOrderMapper INSTANCE = Mappers.getMapper(ReturnOrderMapper.class);
    
    // ReturnOrder -> ReturnOrderDTO
    ReturnOrderDto toReturnOrderDTO(ReturnOrder returnOrder);
    
    // List<ReturnOrder> -> List<ReturnOrderDTO>
    List<ReturnOrderDto> toReturnOrderDTOList(List<ReturnOrder> returnOrders);
    
    // ReturnOrderDetail -> ReturnOrderDetailDTO
    ReturnOrderDetailDto toReturnOrderDetailDTO(ReturnOrderDetail returnOrderDetail);
    
    // List<ReturnOrderDetail> -> List<ReturnOrderDetailDTO>
    List<ReturnOrderDetailDto> toReturnOrderDetailDTOList(List<ReturnOrderDetail> returnOrderDetails);
} 