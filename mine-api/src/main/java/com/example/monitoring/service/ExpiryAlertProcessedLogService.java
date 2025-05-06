package com.example.monitoring.service;

import com.example.monitoring.entity.ExpiryAlertProcessedLog;
import com.example.monitoring.repository.ExpiryAlertProcessedLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 临期预警处理日志服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiryAlertProcessedLogService {

    private final ExpiryAlertProcessedLogRepository expiryAlertProcessedLogRepository;

    /**
     * 标记一个销售批次详情为已处理。
     * 如果已存在处理记录，则不重复创建。
     *
     * @param saleBatchDetailId 要标记为已处理的销售批次详情ID
     * @return 如果成功创建了新的处理日志则返回 true, 如果已存在则返回 false
     */
    @Transactional
    public boolean markAsProcessed(Integer saleBatchDetailId) {
        if (saleBatchDetailId == null) {
            log.warn("尝试标记为已处理的 saleBatchDetailId 为空");
            return false;
        }

        // 检查是否已存在处理记录
        if (expiryAlertProcessedLogRepository.existsBySaleBatchDetailId(saleBatchDetailId)) {
            log.info("销售批次详情 ID: {} 已被标记为处理过，不再重复记录。", saleBatchDetailId);
            return false; // 已处理，无需重复操作
        }

        ExpiryAlertProcessedLog logEntry = new ExpiryAlertProcessedLog(saleBatchDetailId);
        expiryAlertProcessedLogRepository.save(logEntry);
        log.info("销售批次详情 ID: {} 已成功标记为已处理。", saleBatchDetailId);
        return true;
    }

    /**
     * 检查指定的销售批次详情是否已被处理。
     *
     * @param saleBatchDetailId 销售批次详情ID
     * @return 如果已处理则返回 true, 否则返回 false
     */
    @Transactional(readOnly = true)
    public boolean isProcessed(Integer saleBatchDetailId) {
        if (saleBatchDetailId == null) {
            return false; // ID为空，视为未处理
        }
        return expiryAlertProcessedLogRepository.existsBySaleBatchDetailId(saleBatchDetailId);
    }

    /**
     * 撤销对一个销售批次详情的处理标记。
     *
     * @param saleBatchDetailId 要撤销处理标记的销售批次详情ID
     */
    @Transactional
    public void unmarkAsProcessed(Integer saleBatchDetailId) {
        if (saleBatchDetailId == null) {
            log.warn("尝试撤销处理的 saleBatchDetailId 为空，操作被忽略。");
            // 或者可以抛出 IllegalArgumentException("SaleBatchDetailId cannot be null");
            return;
        }
        long deletedCount = expiryAlertProcessedLogRepository.deleteBySaleBatchDetailId(saleBatchDetailId);
        if (deletedCount > 0) {
            log.info("销售批次详情 ID: {} 的处理标记已成功撤销。", saleBatchDetailId);
        } else {
            log.warn("尝试撤销处理的销售批次详情 ID: {}，但未找到对应的处理记录或记录已被删除。", saleBatchDetailId);
            // 根据业务需求，这里可以选择静默处理，或者抛出异常，例如:
            // throw new ProcessedLogNotFoundException("No processed log found for saleBatchDetailId: " + saleBatchDetailId);
        }
    }
}