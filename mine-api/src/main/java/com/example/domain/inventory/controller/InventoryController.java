package com.example.domain.inventory.controller;

import com.example.domain.inventory.dto.InventoryUpdateDto;
import com.example.domain.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存控制器
 * 处理商品库存相关的HTTP请求，包括库存查询、库存变动等
 */
@RestController
@RequestMapping("/inventory")
@Tag(name = "inventory", description = "库存管理接口")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;


    /**
     * 更新库存信息
     *
     * @param inventoryUpdateDto 库存更新信息
     */
    @Operation(summary = "修改库存", description = "根据提供的库存信息进行更新")
    @PostMapping("/update")
    public void update(@RequestBody InventoryUpdateDto inventoryUpdateDto) {
        inventoryService.update(inventoryUpdateDto);

    }





} 