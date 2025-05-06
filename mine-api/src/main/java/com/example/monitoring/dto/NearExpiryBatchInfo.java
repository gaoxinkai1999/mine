package com.example.monitoring.dto; // Updated package

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用于表示临期批次信息的接口投影 (Interface Projection)
 * Getter 方法名需要与 OrderRepository 中 Native Query 的 SQL 列别名匹配。
 */
public interface NearExpiryBatchInfo {

    Integer getShopId(); // 对应 SQL 别名: shopId

    String getShopName(); // 对应 SQL 别名: shopName

    Integer getOrderId(); // 对应 SQL 别名: orderId

    LocalDateTime getLatestOrderTime(); // 对应 SQL 别名: latestOrderTime

    Integer getProductId(); // 对应 SQL 别名: productId

    String getProductName(); // 对应 SQL 别名: productName

    Integer getProductQuantity(); // 对应 SQL 别名: productQuantity

    Integer getBatchId(); // 对应 SQL 别名: batchId

    LocalDate getProductionDate(); // 对应 SQL 别名: productionDate

    Integer getExpiryMonitoringThresholdDays(); // 对应 SQL 别名: expiryMonitoringThresholdDays

    Integer getDaysSinceProduction(); // 对应 SQL 别名: daysSinceProduction

    Integer getOrderDetailId(); // 对应 SQL 别名: orderDetailId

    Integer getSaleBatchDetailId(); // 对应 SQL 别名: saleBatchDetailId

}