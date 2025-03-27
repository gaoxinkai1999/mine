package com.example.domain.purchase.controller;

import com.example.domain.order.dto.PageResponse;
import com.example.domain.product.service.ProductService;
import com.example.domain.purchase.dto.ProductWithPurchaseInfoDto;
import com.example.domain.purchase.dto.PurchaseCreateRequest;
import com.example.domain.purchase.dto.PurchaseDto;
import com.example.domain.purchase.dto.PurchaseListRequest;
import com.example.domain.purchase.entity.Purchase;
import com.example.domain.purchase.mapper.PurchaseMapper;
import com.example.domain.purchase.service.PurchaseService;
import com.example.query.PurchaseQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 采购控制器
 */
@Slf4j
@RestController
@RequestMapping("/purchase")
@Tag(name = "purchase", description = "采购管理接口")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PurchaseMapper purchaseMapper;

    /**
     * 创建采购订单并入库
     *
     * @param request 采购订单创建请求
     * @return 成功信息
     */
    @PostMapping("/create")
    @Operation(summary = "创建采购订单")
    public void createPurchaseOrder(@RequestBody PurchaseCreateRequest request) {
        purchaseService.createPurchaseOrder(request);
    }

    /**
     * 获取采购订单列表，支持分页和筛选
     *
     * @param request 采购订单列表请求
     * @return 分页采购订单列表
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询采购订单列表", 
              description = "支持按状态、时间范围和商品ID筛选采购订单")
    public PageResponse<PurchaseDto> getPurchaseList(@RequestBody PurchaseListRequest request) {
        log.info("查询采购订单列表: {}", request);
        // 构建查询条件
        PurchaseQuery query = PurchaseQuery.builder()
                .state(request.getState())
                .createTimeStart(request.getCreateTimeStart())
                .createTimeEnd(request.getCreateTimeEnd())
                .includes(Set.of(PurchaseQuery.Include.PURCHASE_DETAILS, PurchaseQuery.Include.PRODUCT))
                .build();

        // 分页参数
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize()
        );

        // 执行分页查询
        Slice<Purchase> purchaseSlice = purchaseService.findPage(query, pageable);

        // 转换为DTO并返回
        Slice<PurchaseDto> dtoSlice = purchaseSlice.map(purchaseMapper::toPurchaseDTO);

        return PageResponse.fromSlice(dtoSlice);
    }

    /**
     * 获取采购订单详情
     *
     * @param id 采购订单ID
     * @return 采购订单DTO详情
     */
    @GetMapping("/detail")
    @Operation(summary = "获取采购订单详情")
    public PurchaseDto getPurchaseDetail(@RequestParam Integer id) {
        return purchaseService.getPurchaseDtoById(id)
                             .orElseThrow(() -> new RuntimeException("采购订单不存在"));
    }

    /**
     * 取消采购订单
     *
     * @param purchaseId 采购订单 ID
     * @return 成功信息
     */
    @PostMapping("/cancel")
    @Operation(summary = "取消采购订单")
    public void cancelPurchaseOrder(@RequestParam Integer purchaseId) {
        purchaseService.cancelPurchaseOrder(purchaseId);
    }

    /**
     * 获取包含采购信息的在售商品列表
     */
    @GetMapping("/getOnSaleProductsWithPurchaseInfo")
    @Operation(summary = "获取包含采购信息的在售商品列表")
    public List<ProductWithPurchaseInfoDto> getOnSaleProductsWithPurchaseInfo() {
        return purchaseService.getOnSaleProductsWithPurchaseInfo();
    }
}
