package com.example.domain.order.controller;

import com.example.domain.order.dto.OrderCreateRequest;
import com.example.domain.order.dto.OrderDto;
import com.example.domain.order.dto.OrderListRequest;
import com.example.domain.order.dto.PageResponse;
import com.example.domain.order.entity.Order;
import com.example.domain.order.mapper.OrderMapper;
import com.example.domain.order.service.OrderService;
import com.example.query.OrderQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 订单控制器
 * 处理订单相关的HTTP请求
 */
@Slf4j
@RestController
@RequestMapping("/order")
@Tag(name = "order", description = "订单处理")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;



    /**
     * 查询订单列表，支持分页和筛选
     *
     * @param request 订单列表请求
     * @return 分页订单列表
     */
    @Operation(summary = "分页查询订单列表",
            description = "支持按店铺和时间范围筛选订单，默认关联加载店铺信息")
    @PostMapping("/list")
    public PageResponse<OrderDto> getOrders(@RequestBody OrderListRequest request) {
        log.info("查询订单列表: {}", request);
        // 构建查询条件
        OrderQuery query = OrderQuery.builder()
                                     .startTime(request.getStartDate())
                                     .endTime(request.getEndDate())
                                     .shopId(request.getShopId())
                                     .includes(Set.of(OrderQuery.Include.SHOP, OrderQuery.Include.DETAILS, OrderQuery.Include.PRODUCT))
                                     .build();

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize()
        );


        // 执行分页查询
        Slice<Order> orderSlice = orderService.findPage(query, pageable);

        // 转换为DTO并返回
        Slice<OrderDto> dtoSlice = orderSlice.map(orderMapper::toOrderDTO);

        return PageResponse.fromSlice(dtoSlice);
    }


    /**
     * 创建订单
     *
     * @param request 订单创建请求
     */
    @Operation(summary = "新建订单", description = "根据购物车信息创建订单")
    @PostMapping("/create")
    public void createOrder(@RequestBody OrderCreateRequest request) {
        log.info("创建订单: {}", request);
        orderService.createOrder(request);
    }

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     */
    @PostMapping("/cancel")
    public void cancelOrder(@RequestParam Integer orderId) {
        orderService.cancelOrder(orderId);
    }


}
