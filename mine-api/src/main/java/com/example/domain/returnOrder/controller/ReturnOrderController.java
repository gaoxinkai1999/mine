package com.example.domain.returnOrder.controller;

import com.example.domain.order.dto.PageResponse;
import com.example.domain.returnOrder.dto.ReturnOrderDto;
import com.example.domain.returnOrder.dto.ReturnOrderListRequest;
import com.example.domain.returnOrder.dto.ReturnOrderRequest;
import com.example.domain.returnOrder.entity.ReturnOrder;
import com.example.domain.returnOrder.mapper.ReturnOrderMapper;
import com.example.domain.returnOrder.service.ReturnOrderService;
import com.example.query.ReturnOrderQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 退货订单控制器
 * 处理退货订单相关的HTTP请求
 */
@Slf4j
@RestController
@Tag(name = "returnOrder", description = "退货订单管理接口")
@RequestMapping("/returnOrder")
public class ReturnOrderController {

    @Autowired
    private ReturnOrderService returnOrderService;
    
    @Autowired
    private ReturnOrderMapper returnOrderMapper;

    /**
     * 创建退货订单
     *
     * @param request 退货订单请求
     * @return 创建的退货订单
     */
    @PostMapping("/create")
    @Operation(summary = "创建退货订单", description = "创建退货订单")
    public void createReturnOrder(@Valid @RequestBody ReturnOrderRequest request) {
         returnOrderService.createReturnOrder(request);

    }

    /**
     * 根据ID查询退货订单
     *
     * @param id 退货订单ID
     * @return 退货订单
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询退货订单", description = "根据ID查询退货订单")
    public ReturnOrderDto getReturnOrderById(@PathVariable Integer id) {
        return returnOrderService.getReturnOrderById(id);

    }

    /**
     * 查询退货订单列表，支持分页
     *
     * @param request 退货订单列表请求
     * @return 分页退货订单列表
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询退货订单列表", description = "查询退货订单列表，支持分页和筛选")
    public PageResponse<ReturnOrderDto> getReturnOrders(@RequestBody ReturnOrderListRequest request) {
        log.info("查询退货订单列表: {}", request);
        
        // 构建查询条件
        ReturnOrderQuery query = ReturnOrderQuery.builder()
                .shopId(request.getShopId())
                .startTime(request.getStartDate())
                .endTime(request.getEndDate())
                .includes(Set.of(ReturnOrderQuery.Include.SHOP, ReturnOrderQuery.Include.DETAILS))
                .build();
        
        // 创建分页对象
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize()
        );
        
        // 执行分页查询
        Slice<ReturnOrder> returnOrderSlice = returnOrderService.findPage(query, pageable);
        
        // 转换为DTO并返回
        Slice<ReturnOrderDto> dtoSlice = returnOrderSlice.map(returnOrderMapper::toReturnOrderDTO);
        
        return PageResponse.fromSlice(dtoSlice);
    }

    /**
     * 删除退货订单
     *
     * @param id 退货订单ID
     * @return 无内容响应
     */
    @PostMapping("/delete/{id}")
    @Operation(summary = "删除退货订单", description = "删除退货订单")
    public void deleteReturnOrder(@PathVariable Integer id) {
        returnOrderService.deleteReturnOrder(id);
    }
    
    /**
     * 查询退货订单列表（兼容旧API）
     *
     * @param shopId    商店ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @return 退货订单列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询退货订单列表（旧API）", description = "查询退货订单列表，不支持分页")
    public List<ReturnOrderDto> getReturnOrdersLegacy(
            @RequestParam(required = false) Integer shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return returnOrderService.getReturnOrders(shopId, startDate, endDate);
    }
}