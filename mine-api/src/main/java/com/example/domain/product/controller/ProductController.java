package com.example.domain.product.controller;

import com.example.domain.product.dto.ProductDto;
import com.example.domain.product.dto.ProductUpdateDto;
import com.example.domain.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 * 处理商品相关的HTTP请求，包括查询、更新和添加商品
 */
@Slf4j
@Tag(name = "product", description = "处理商品相关的HTTP请求，包括查询、更新和添加商品")
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;


    /**
     * 软删除商品
     * @param productId
     */
    @PostMapping("/deleteProduct")
    public void deleteProduct(@RequestParam Integer productId) {
        productService.deleteProduct(productId);
    }
    /**
     * 查询所有商品
     */
    @Operation(summary = "查询所有商品", description = "获取所有商品的详细信息")
    @PostMapping("/getProducts")
    public List<ProductDto> getProducts() {
        return productService.getProducts();

    }
    /**
     * 新建商品
     * @return
     */
    @PostMapping("/createProduct")
    @Operation(summary = "新建商品", description = "新建商品")
    public void createProduct(@RequestBody ProductDto productDto) {
        // log.info("创建商品: {}", productDto);
        productService.createProduct(productDto);
    }



    /**
     * 批量更新产品信息
     *
     * @param products 产品更新请求列表
     */
    @Operation(summary = "批量更新产品信息")
    @PostMapping("/batch-update")
    public void batchUpdate(@RequestBody List<ProductUpdateDto> products) {
        productService.batchUpdate(products);
    }

}
