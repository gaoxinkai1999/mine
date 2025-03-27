# 采购预测服务优化设计方案 (v1.0)

## 1. 目标

实现一个灵活、可扩展的采购预测服务，能够根据商品历史销售数据的数量，自动选择最合适的预测算法（策略），以提高预测的准确性和适应性。

## 2. 核心设计：策略模式 + Spring依赖注入

采用**策略模式**，将不同的预测算法封装成独立的策略类。利用**Spring的依赖注入**机制，将所有可用的预测策略注入到核心的`ForecastService`中，由`ForecastService`根据数据量动态选择并执行相应的策略。

## 3. 主要组件

### 3.1. `ForecastStrategy` 接口

定义所有预测策略必须实现的通用接口。

```java
package com.example.domain.forecast.strategy;

import com.example.exception.MyException;

/**
 * 预测策略接口
 */
public interface ForecastStrategy {

    /**
     * 执行预测
     * @param historicalData 清洗和平滑后的历史数据
     * @param forecastDays 需要预测的未来天数
     * @return 每日预测结果数组
     * @throws MyException 预测过程中发生错误
     */
    double[] forecast(double[] historicalData, int forecastDays) throws MyException;

    /**
     * 判断该策略是否适用于给定的历史数据量
     * @param dataLength 历史数据的长度
     * @return true 如果适用，false 如果不适用
     */
    boolean canHandle(int dataLength);

    /**
     * 策略的名称 (可选, 用于日志或调试)
     * @return 策略名称
     */
    String getStrategyName();
    
    /**
     * 获取策略优先级或适用的最小数据长度 (用于排序)
     * @return 优先级数值，数值越大（或越小，根据约定）优先级越高
     */
    // int getPriority(); // 或者 getMinDataLength();
}
```
*(注：添加了 `getPriority()` 或 `getMinDataLength()` 的注释，用于后续策略排序)*

### 3.2. 具体预测策略实现

根据数据量阈值，实现多个策略类，每个类负责一种预测算法。所有策略类都需要实现`ForecastStrategy`接口，并注册为Spring Bean (例如使用 `@Component` 或 `@Service`)。

*   **`HoltWintersSeasonalStrategy`**:
    *   **算法**: Holt-Winters 三重指数平滑 (季节性模型)
    *   **适用条件**: `canHandle(dataLength)` 返回 `true` 当 `dataLength >= 365` (至少一年数据)。
    *   **实现**: 调用重构后的Holt-Winters季节性预测核心逻辑，可能包含参数优化。
    *   **名称**: "Holt-Winters Seasonal"
    *   **优先级/最小数据量**: 365

*   **`HoltNonSeasonalStrategy`**:
    *   **算法**: Holt 双指数平滑 (非季节性模型)
    *   **适用条件**: `canHandle(dataLength)` 返回 `true` 当 `90 <= dataLength < 365` (3个月到1年数据)。
    *   **实现**: 调用重构后的Holt非季节性预测核心逻辑，可能包含参数优化。
    *   **名称**: "Holt Non-Seasonal"
    *   **优先级/最小数据量**: 90

*   **`MovingAverageStrategy`**:
    *   **算法**: 简单移动平均 (或其他移动平均变种)
    *   **适用条件**: `canHandle(dataLength)` 返回 `true` 当 `30 <= dataLength < 90` (1个月到3个月数据)。
    *   **实现**: 实现移动平均预测逻辑。
    *   **名称**: "Moving Average"
    *   **优先级/最小数据量**: 30

*   **`RecentAverageStrategy` (或 `DefaultFallbackStrategy`)**:
    *   **算法**: 取最近N天销量的平均值作为未来预测值，或返回固定值/零。
    *   **适用条件**: `canHandle(dataLength)` 返回 `true` 当 `dataLength < 30` (数据量过少)。
    *   **实现**: 实现简单的平均值计算或默认值逻辑。
    *   **名称**: "Recent Average / Fallback"
    *   **优先级/最小数据量**: 0

*(**注意**: 上述数据量阈值为示例，可根据实际业务效果调整)*

### 3.3. `ForecastService` (核心服务)

负责协调整个预测流程。

```java
package com.example.domain.forecast.service;

import com.example.domain.forecast.strategy.ForecastStrategy;
import com.example.domain.product.service.ProductService; // 假设注入
import com.example.domain.statistics.dto.response.ProductSalesInfoDTO; // 假设引入
import com.example.domain.statistics.dto.response.SalesStatisticsDTO; // 假设引入
import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ForecastService {

    private final List<ForecastStrategy> strategies;
    
    @Autowired 
    private ProductService productService; 

    @Autowired
    public ForecastService(List<ForecastStrategy> strategies) {
        // 按处理数据量要求从大到小排序策略，优先匹配更复杂的模型
        // 假设 ForecastStrategy 提供了 getMinDataLength() 方法
        this.strategies = strategies.stream()
            .sorted(Comparator.comparingInt(ForecastStrategy::getMinDataLength).reversed()) // 示例排序
            .toList();
        log.info("加载并排序的预测策略: {}", this.strategies.stream().map(ForecastStrategy::getStrategyName).collect(Collectors.joining(", ")));
    }

    /**
     * 预测单个商品的未来销量总和
     * @param productId 商品ID
     * @param historicalDataMap 包含历史销售数据的Map (日期 -> 统计DTO)
     * @param forecastDays 预测天数
     * @return 预测的未来N天总销量
     * @throws MyException 预测失败
     */
    public double forecastProductTotal(int productId, Map<LocalDate, SalesStatisticsDTO> historicalDataMap, int forecastDays) throws MyException {
        // 1. 提取该商品的原始日销量数据
        double[] rawDailySales = getProductData(historicalDataMap, productId);
        log.debug("商品ID: {}, 原始数据长度: {}", productId, rawDailySales.length);

        if (rawDailySales.length == 0) {
             log.warn("商品ID: {} 历史数据为空，无法预测。", productId);
             return 0.0; // 或根据业务返回特定值/抛异常
        }

        // 2. 数据预处理 (例如：7日移动平均平滑) - 可选步骤
        double[] processedData = applyDataPreprocessing(rawDailySales);
        log.debug("商品ID: {}, 处理后数据长度: {}", productId, processedData.length);


        // 3. 选择合适的预测策略 (已排序，取第一个满足条件的)
        ForecastStrategy selectedStrategy = strategies.stream()
            .filter(strategy -> strategy.canHandle(processedData.length))
            .findFirst()
            .orElseThrow(() -> new MyException("商品ID: " + productId + " 没有找到适合数据量 " + processedData.length + " 的预测策略"));
        log.info("商品ID: {}, 数据长度: {}, 选择策略: {}", productId, processedData.length, selectedStrategy.getStrategyName());

        // 4. 执行预测
        double[] dailyForecast;
        try {
            dailyForecast = selectedStrategy.forecast(processedData, forecastDays);
        } catch (Exception e) {
            log.error("商品ID: {} 使用策略 {} 预测失败: {}", productId, selectedStrategy.getStrategyName(), e.getMessage(), e);
            // 可选：尝试降级到更简单的策略或返回默认值
            throw new MyException("商品ID: " + productId + " 预测失败", e);
        }


        // 5. 计算并返回总预测量
        double totalForecast = Arrays.stream(dailyForecast).sum();
        log.debug("商品ID: {}, 预测未来 {} 天总销量: {}", productId, forecastDays, totalForecast);
        
        // 确保预测结果非负
        return Math.max(0, totalForecast);
    }
    
    /**
     * 数据预处理：应用7日移动平均 (示例)
     * @param rawData 原始日销量数据
     * @return 处理后的数据
     */
    private double[] applyDataPreprocessing(double[] rawData) {
        // 这里可以调用实际的平滑方法，例如:
        // return MovingAverageCalculator.calculateSimpleMovingAverage(rawData, 7); // 假设有这个工具类
        // 或者直接返回原始数据，如果不需要预处理:
         log.debug("数据预处理步骤：暂未应用平滑处理。");
         return rawData; 
    }

    /**
     * 从统计数据中提取指定商品的日销量数组
     * @param data 历史统计数据 Map
     * @param productId 商品ID
     * @return 该商品的日销量数组
     */
    public double[] getProductData(Map<LocalDate, SalesStatisticsDTO> data, int productId) {
        LocalDate earliestSaleDate = productService.getEarliestSaleDateByProductId(productId);
        if (earliestSaleDate == null) {
             log.warn("无法获取商品ID: {} 的最早销售日期，可能无销售记录。", productId);
             return new double[0];
        }
        
        // 过滤并排序日期
        List<Map.Entry<LocalDate, SalesStatisticsDTO>> sortedEntries = data.entrySet()
                                                                  .stream()
                                                                  .filter(entry -> !entry.getKey().isBefore(earliestSaleDate)) 
                                                                  .sorted(Map.Entry.comparingByKey()) 
                                                                  .toList();
        
        // 提取销量
        double[] array = sortedEntries.stream()
                             .mapToDouble(entry -> 
                                 entry.getValue()
                                      .getProductSalesInfoDTOS() // 获取产品销售列表
                                      .stream()
                                      .filter(dto -> dto.getProductId() == productId) // 找到对应商品
                                      .mapToInt(ProductSalesInfoDTO::getQuantity) // 获取销量
                                      .findFirst() // 找到第一个（理论上只有一个）
                                      .orElse(0) // 如果当天没卖，销量为0
                             )
                             .toArray();
        return array;
    }
    
    // ... 其他 ForecastService 中的方法 ...
}

```
*(注：在`ForecastService`示例代码中添加了日志、排序逻辑、错误处理和更详细的注释)*

### 3.4. `HoltWintersForecast` 类重构 (建议)

*   将此类重构为更侧重于提供**核心算法实现**的工具类或服务。
*   移除 `dailyForecast` 方法中的模型选择逻辑（交给`ForecastService`）。
*   移除 `dailyForecast` 方法中的数据平滑逻辑（交给`ForecastService`或策略本身）。
*   提供独立的、清晰的季节性预测方法和非季节性预测方法供策略类调用。
*   参数优化方法可以保留，供策略类在执行预测前调用。

## 4. 工作流程

1.  **触发预测**: `PurchaseService` 调用 `ForecastService.forecastProductTotal()` 并传入商品ID、历史数据Map和预测天数。
2.  **数据提取**: `ForecastService` 调用 `getProductData()` 从历史数据Map中提取指定商品的日销量数组。
3.  **数据预处理 (可选)**: `ForecastService` 对提取的日销量数据进行平滑处理（如移动平均）。
4.  **策略选择**: `ForecastService` 遍历**已排序**的 `ForecastStrategy` 列表，根据处理后的数据长度，调用每个策略的 `canHandle()` 方法，找到第一个返回 `true` 的策略（因为已排序，这将是满足条件的最优策略）。
5.  **执行预测**: `ForecastService` 调用选定策略的 `forecast()` 方法，传入处理后的数据和预测天数。
6.  **结果处理**: `ForecastService` 获取策略返回的每日预测结果数组，计算总和（或按需处理），并返回最终结果。

## 5. 设计优势

*   **灵活性和可扩展性**: 新增预测算法只需实现`ForecastStrategy`接口并注册为Bean即可，无需修改`ForecastService`。
*   **职责清晰**: `ForecastService`负责流程编排和策略选择，每个策略类负责具体的算法实现。
*   **可维护性**: 代码结构清晰，易于理解和修改。
*   **可测试性**: 每个预测策略都可以独立进行单元测试。
*   **适应性**: 能根据数据量的不同自动选用最合适的模型。

## 6. 未来可能的增强

*   实现更复杂的策略选择逻辑（例如基于数据特征、历史预测效果等）。
*   引入策略组合或加权平均。
*   增加对预测结果的置信区间评估。
*   实现预测模型的自动更新和参数调优。
*   将数据预处理步骤也策略化或配置化。
