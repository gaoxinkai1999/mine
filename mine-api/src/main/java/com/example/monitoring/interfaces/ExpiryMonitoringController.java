package com.example.monitoring.interfaces; // Updated package

import com.example.monitoring.dto.NearExpiryBatchInfo; // Updated DTO import
import com.example.monitoring.service.ExpiryMonitoringService; // Updated Service import
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品临期监控控制器
 */
@Tag(name = "monitoring", description = "商品监控相关接口")
@RestController
@RequestMapping("/api/monitor") // 统一的监控API前缀
@RequiredArgsConstructor
public class ExpiryMonitoringController {

    private final ExpiryMonitoringService expiryMonitoringService;

    @GetMapping("/expiry/batches") // 修改路径以反映是批次信息
    @Operation(summary = "获取临期批次列表", description = "对于每个商家，查找其最后一次包含批次管理商品的销售订单，并检查该订单中每个批次的生产日期是否超过对应商品的临期阈值。")
    public ResponseEntity<List<NearExpiryBatchInfo>> getNearExpiryBatches() {
        List<NearExpiryBatchInfo> nearExpiryBatches = expiryMonitoringService.findNearExpiryBatches(); // 调用新的 Service 方法
        return ResponseEntity.ok(nearExpiryBatches);
    }
}