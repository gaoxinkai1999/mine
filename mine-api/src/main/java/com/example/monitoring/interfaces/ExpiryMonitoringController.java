package com.example.monitoring.interfaces; // Updated package

import com.example.monitoring.dto.NearExpiryBatchInfo; // Updated DTO import
import com.example.monitoring.service.ExpiryAlertProcessedLogService; // 新增 Service 依赖
import com.example.monitoring.service.ExpiryMonitoringService; // Updated Service import
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // 导入 Page
import org.springframework.data.domain.Pageable; // 导入 Pageable
import org.springframework.data.web.PageableDefault; // 导入 PageableDefault
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.monitoring.dto.ProcessedItemDetailDto; // 导入新的 DTO
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
    private final ExpiryAlertProcessedLogService expiryAlertProcessedLogService; // 注入新的 Service

    @GetMapping("/expiry/batches") // 修改路径以反映是批次信息
    @Operation(summary = "获取临期批次列表", description = "对于每个商家，查找其最后一次包含批次管理商品的销售订单，并检查该订单中每个批次的生产日期是否超过对应商品的临期阈值。")
    public List<NearExpiryBatchInfo> getNearExpiryBatches() {
        return expiryMonitoringService.findNearExpiryBatches(); // 直接返回列表
    }

    @PostMapping("/expiry/items/{saleBatchDetailId}/process")
    @Operation(summary = "标记临期商品条目为已处理", description = "根据销售批次详情ID (saleBatchDetailId) 将其标记为已处理，处理后该条目将不再出现在临期列表中。")
    public void markExpiryItemAsProcessed(
            @Parameter(description = "要标记为已处理的销售批次详情ID", required = true)
            @PathVariable Integer saleBatchDetailId) {
       expiryAlertProcessedLogService.markAsProcessed(saleBatchDetailId);
       // 方法为 void，Spring MVC 默认返回 HTTP 200 OK
    }

    @GetMapping("/expiry/processed-items")
    @Operation(summary = "获取已处理的临期商品历史列表", description = "分页查询已被标记为处理的临期商品项。")
    public Page<ProcessedItemDetailDto> getProcessedItemHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        // 排序已在 Repository 的 @Query 中通过 ORDER BY eapl.processed_at DESC 实现
        return expiryMonitoringService.getProcessedItemHistory(pageable);
    }

    @PostMapping("/expiry/items/{saleBatchDetailId}/unprocess") // 使用 POST 根据用户要求
    @Operation(summary = "撤销临期商品条目的处理标记", description = "根据销售批次详情ID (saleBatchDetailId) 将其处理标记撤销，使其可能重新出现在临期列表中。")
    public void unmarkExpiryItemAsProcessed(
            @Parameter(description = "要撤销处理标记的销售批次详情ID", required = true)
            @PathVariable Integer saleBatchDetailId) {
        expiryAlertProcessedLogService.unmarkAsProcessed(saleBatchDetailId);
        // 方法为 void，Spring MVC 默认返回 HTTP 200 OK
    }
}