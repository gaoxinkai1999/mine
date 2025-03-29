package com.example.domain.order.controller;

import com.example.domain.order.dto.PageResponse;
import com.example.domain.order.dto.ProductOrderItemDto;
import com.example.domain.order.dto.ProductOrderListRequest;
import com.example.domain.order.entity.OrderDetail;
import com.example.domain.order.mapper.OrderDetailMapper;
import com.example.domain.order.service.OrderDetailService;
import com.example.query.OrderDetailQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 产品订单控制器
 * 处理产品订单相关的HTTP请求
 */
@Slf4j
@RestController
@RequestMapping("/product-order")
@Tag(name = "product-order", description = "产品订单查询")
public class ProductOrderController {

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    /**
     * 获取产品的销售订单项列表
     * 
     * @param request 查询请求
     * @return 分页的产品订单项列表
     */
    @Operation(summary = "获取产品销售订单项列表", 
            description = "根据产品ID查询该商品的所有销售订单项，支持分页和时间范围筛选")
    @PostMapping("/list")
    public PageResponse<ProductOrderItemDto> getProductOrderList(@RequestBody ProductOrderListRequest request) {
        log.info("查询产品销售订单项列表: {}", request);
        
        // 构建查询条件
        OrderDetailQuery query = OrderDetailQuery.builder()
                .productId(request.getProductId())
                .startTime(request.getStartDate())
                .endTime(request.getEndDate())
                .includes(Set.of(OrderDetailQuery.Include.ORDER, OrderDetailQuery.Include.PRODUCT, OrderDetailQuery.Include.SHOP))
                .build();
        
        // 创建分页对象
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize()
        );
        
        // 执行分页查询
        Slice<OrderDetail> orderDetailSlice = orderDetailService.findPage(query, pageable);
        
        // 转换为DTO并返回
        Slice<ProductOrderItemDto> dtoSlice = orderDetailSlice.map(orderDetailMapper::toProductOrderItemDto);
        
        return PageResponse.fromSlice(dtoSlice);
    }
} 