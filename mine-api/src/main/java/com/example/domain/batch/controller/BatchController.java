package com.example.domain.batch.controller;

import com.example.domain.batch.dto.BatchUpdateDto;
import com.example.domain.batch.entity.Batch;
import com.example.domain.batch.service.BatchService;
import com.example.query.BatchQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 批次控制器
 * 处理商品批次相关的HTTP请求，包括查询批次信息、批次状态管理等
 */
@RestController
@RequestMapping("/batch")
@Tag(name = "batch", description = "批次管理接口")
public class BatchController {

    @Autowired
    private BatchService batchService;

    /**
     * 查询批次列表
     *
     * @param query 查询条件
     * @return 批次列表
     */
    @Operation(summary = "查询批次列表", description = "根据查询条件获取批次列表")
    @PostMapping("/list")
    public List<Batch> getBatches(@RequestBody BatchQuery query) {
        // 默认加载商品信息
        if (query.getIncludes() == null) {
            query.setIncludes(Set.of(BatchQuery.Include.PRODUCT));
        }
        return batchService.findList(query);
    }

    /**
     * 查询商品的有效批次
     *
     * @param productId 商品ID
     * @return 有效批次列表
     */
    @Operation(summary = "查询商品有效批次", description = "查询指定商品的所有有效批次")
    @GetMapping("/product/{productId}/valid")
    public List<Batch> getValidBatches(@PathVariable Integer productId) {
        return batchService.findValidBatches(productId);
    }

    /**
     * 查询商品的所有批次
     *
     * @param productId 商品ID
     * @param status 批次状态（可选）
     * @return 批次列表
     */
    @Operation(summary = "查询商品所有批次", description = "查询指定商品的所有批次，可按状态筛选")
    @GetMapping("/product/{productId}")
    public List<Batch> getProductBatches(
        @PathVariable Integer productId,
        @RequestParam(required = false) Boolean status
    ) {
        return batchService.findByProduct(productId, status);
    }




    /**
     * 批量更新批次信息
     *
     * @param batches 批次更新请求列表
     */
    @Operation(summary = "批量更新批次信息")
    @PostMapping("/batch-update")
    public void batchUpdate(@RequestBody List<BatchUpdateDto> batches) {
        batchService.batchUpdate(batches);
    }
} 