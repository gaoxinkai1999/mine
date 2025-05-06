package com.example.monitoring.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime; // 导入 LocalDateTime

/**
 * 临期预警处理日志实体
 * 记录哪些 sale_batch_detail 已被处理
 */
@Entity
@Table(name = "expiry_alert_processed_log",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"sale_batch_detail_id"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryAlertProcessedLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sale_batch_detail_id", nullable = false)
    private Integer saleBatchDetailId;

    @Column(name = "processed_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime processedAt;

    // 如果需要关联到 SaleBatchDetail 实体，可以取消以下注释
    // @OneToOne
    // @JoinColumn(name = "sale_batch_detail_id", referencedColumnName = "id", insertable = false, updatable = false)
    // private SaleBatchDetail saleBatchDetail;


    public ExpiryAlertProcessedLog(Integer saleBatchDetailId) {
        this.saleBatchDetailId = saleBatchDetailId;
        // this.processedAt 字段将由数据库的 DEFAULT CURRENT_TIMESTAMP 自动填充
        // 或者如果使用 @CreationTimestamp (Hibernate)，则由 Hibernate 填充
    }

    // Getter for processedAt might be useful, Lombok @Data should generate it.
    // No setter for processedAt as it's updatable = false.
}