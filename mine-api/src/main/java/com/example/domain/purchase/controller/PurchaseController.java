package com.example.domain.purchase.controller;

import com.example.domain.product.service.ProductService;
import com.example.domain.purchase.dto.ProductWithPurchaseInfoDto;
import com.example.domain.purchase.dto.PurchaseCreateRequest;
import com.example.domain.purchase.service.PurchaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购控制器
 */
@RestController
@RequestMapping("/purchase")
@Tag(name = "purchase", description = "采购管理接口")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ProductService productService;

    /**
     * 创建采购订单并入库
     *
     * @param request 采购订单创建请求
     * @return 成功信息
     */
    @PostMapping("/create")
    public void createPurchaseOrder(@RequestBody PurchaseCreateRequest request) {
        purchaseService.createPurchaseOrder(request);
    }

    /**
     * 取消采购订单
     *
     * @param purchaseId 采购订单 ID
     * @return 成功信息
     */
    @PostMapping("/cancel")
    public void cancelPurchaseOrder(@RequestParam Integer purchaseId) {
        purchaseService.cancelPurchaseOrder(purchaseId);

    }


    // 获取包含采购信息的在售商品列表
    @GetMapping("/getOnSaleProductsWithPurchaseInfo")
    public List<ProductWithPurchaseInfoDto> getOnSaleProductsWithPurchaseInfo() {
       return purchaseService.getOnSaleProductsWithPurchaseInfo();
    }


    /**
     * 删除入库单
     *
     * @param id 入库单ID
     * @return 成功信息
     */
    @PostMapping("/delete")
    public String delete(@RequestParam Integer id) {
        purchaseService.cancelPurchaseOrder(id);
        return "采购订单删除成功！";
    }
}
