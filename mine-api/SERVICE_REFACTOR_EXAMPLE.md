# 大型服务类拆分示例：销售预测服务

本文档展示如何将一个大型服务类（如`SalesForecastService`）拆分成多个职责单一的小型服务类。

## 问题分析

当前的`SalesForecastService`有1227行代码，过于庞大，承担了太多责任：
- 数据准备和清洗
- 时间序列分析
- 多种预测算法的实现
- 预测结果处理和展示
- 辅助功能（如导出、日志记录等）

这种设计违反了单一职责原则，导致代码难以维护和测试。

## 拆分策略

我们将按照功能职责将`SalesForecastService`拆分为以下几个服务类：

1. `ForecastDataService` - 负责数据准备和清洗
2. `TimeSeriesAnalysisService` - 负责时间序列分析
3. `BasicForecastService` - 提供基本的预测算法实现
4. `AdvancedForecastService` - 提供高级预测算法实现
5. `ForecastResultService` - 处理预测结果
6. `ForecastExportService` - 处理结果导出

## 拆分实施

### 步骤 1：创建服务目录

```shell
mkdir -p src/main/java/com/example/domain/statistics/service/forecast
```

### 步骤 2：创建基础接口

```java
// ForecastService.java
package com.example.domain.statistics.service.forecast;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 销售预测服务的基础接口
 */
public interface ForecastService {
    /**
     * 执行销售预测
     * @param productId 产品ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param parameters 预测参数
     * @return 预测结果
     */
    Map<LocalDate, Double> forecast(Long productId, LocalDate startDate, LocalDate endDate, Map<String, Object> parameters);
}
```

### 步骤 3：创建数据准备服务

```java
// ForecastDataService.java
package com.example.domain.statistics.service.forecast;

import com.example.domain.order.entity.OrderDetail;
import com.example.domain.order.repository.OrderRepository;
import com.example.domain.product.entity.Product;
import com.example.domain.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 负责预测数据的准备和清洗
 */
@Service
public class ForecastDataService {
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * 获取历史销售数据
     * @param productId 产品ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每天的销售数量
     */
    public Map<LocalDate, Double> getHistoricalSalesData(Long productId, LocalDate startDate, LocalDate endDate) {
        // 获取历史销售数据的实现代码
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    /**
     * 处理数据异常值
     * @param salesData 销售数据
     * @return 处理后的销售数据
     */
    public Map<LocalDate, Double> handleOutliers(Map<LocalDate, Double> salesData) {
        // 异常值处理逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    /**
     * 填补缺失数据
     * @param salesData 销售数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 填补后的销售数据
     */
    public Map<LocalDate, Double> fillMissingData(Map<LocalDate, Double> salesData, LocalDate startDate, LocalDate endDate) {
        // 缺失数据填补逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
}
```

### 步骤 4：创建时间序列分析服务

```java
// TimeSeriesAnalysisService.java
package com.example.domain.statistics.service.forecast;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 负责时间序列的分析
 */
@Service
public class TimeSeriesAnalysisService {
    /**
     * 分析销售趋势
     * @param salesData 销售数据
     * @return 趋势分析结果
     */
    public double[] analyzeTrend(Map<LocalDate, Double> salesData) {
        // 趋势分析逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    /**
     * 分析季节性
     * @param salesData 销售数据
     * @return 季节性分析结果
     */
    public double[] analyzeSeasonality(Map<LocalDate, Double> salesData) {
        // 季节性分析逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    /**
     * 计算自相关系数
     * @param salesData 销售数据
     * @param lag 滞后期数
     * @return 自相关系数
     */
    public double calculateAutocorrelation(Map<LocalDate, Double> salesData, int lag) {
        // 自相关计算逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
}
```

### 步骤 5：创建基本预测服务

```java
// BasicForecastService.java
package com.example.domain.statistics.service.forecast;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 提供基本的预测算法实现
 */
@Service
public class BasicForecastService implements ForecastService {
    @Autowired
    private ForecastDataService dataService;
    
    @Autowired
    private TimeSeriesAnalysisService analysisService;
    
    /**
     * 执行移动平均预测
     * @param salesData 销售数据
     * @param windowSize 窗口大小
     * @param forecastPeriod 预测周期
     * @return 预测结果
     */
    public Map<LocalDate, Double> movingAverageForecast(Map<LocalDate, Double> salesData, int windowSize, int forecastPeriod) {
        // 移动平均预测逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    /**
     * 执行指数平滑预测
     * @param salesData 销售数据
     * @param alpha 平滑系数
     * @param forecastPeriod 预测周期
     * @return 预测结果
     */
    public Map<LocalDate, Double> exponentialSmoothingForecast(Map<LocalDate, Double> salesData, double alpha, int forecastPeriod) {
        // 指数平滑预测逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    @Override
    public Map<LocalDate, Double> forecast(Long productId, LocalDate startDate, LocalDate endDate, Map<String, Object> parameters) {
        // 获取历史数据
        Map<LocalDate, Double> historicalData = dataService.getHistoricalSalesData(productId, startDate.minusYears(1), startDate.minusDays(1));
        
        // 处理数据
        historicalData = dataService.handleOutliers(historicalData);
        historicalData = dataService.fillMissingData(historicalData, startDate.minusYears(1), startDate.minusDays(1));
        
        // 执行预测
        String method = (String) parameters.getOrDefault("method", "movingAverage");
        int forecastPeriod = endDate.compareTo(startDate);
        
        if ("movingAverage".equals(method)) {
            int windowSize = (int) parameters.getOrDefault("windowSize", 7);
            return movingAverageForecast(historicalData, windowSize, forecastPeriod);
        } else if ("exponentialSmoothing".equals(method)) {
            double alpha = (double) parameters.getOrDefault("alpha", 0.3);
            return exponentialSmoothingForecast(historicalData, alpha, forecastPeriod);
        } else {
            throw new IllegalArgumentException("Unsupported forecast method: " + method);
        }
    }
}
```

### 步骤 6：创建高级预测服务

```java
// AdvancedForecastService.java
package com.example.domain.statistics.service.forecast;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 提供高级预测算法实现
 */
@Service
public class AdvancedForecastService implements ForecastService {
    @Autowired
    private ForecastDataService dataService;
    
    @Autowired
    private TimeSeriesAnalysisService analysisService;
    
    /**
     * 执行ARIMA预测
     * @param salesData 销售数据
     * @param p 自回归阶数
     * @param d 差分阶数
     * @param q 移动平均阶数
     * @param forecastPeriod 预测周期
     * @return 预测结果
     */
    public Map<LocalDate, Double> arimaForecast(Map<LocalDate, Double> salesData, int p, int d, int q, int forecastPeriod) {
        // ARIMA预测逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    /**
     * 执行Holt-Winters季节性预测
     * @param salesData 销售数据
     * @param alpha 水平平滑系数
     * @param beta 趋势平滑系数
     * @param gamma 季节平滑系数
     * @param seasonLength 季节长度
     * @param forecastPeriod 预测周期
     * @return 预测结果
     */
    public Map<LocalDate, Double> holtWintersForecast(Map<LocalDate, Double> salesData, 
                                              double alpha, double beta, double gamma, 
                                              int seasonLength, int forecastPeriod) {
        // Holt-Winters预测逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    @Override
    public Map<LocalDate, Double> forecast(Long productId, LocalDate startDate, LocalDate endDate, Map<String, Object> parameters) {
        // 获取历史数据
        Map<LocalDate, Double> historicalData = dataService.getHistoricalSalesData(productId, startDate.minusYears(1), startDate.minusDays(1));
        
        // 处理数据
        historicalData = dataService.handleOutliers(historicalData);
        historicalData = dataService.fillMissingData(historicalData, startDate.minusYears(1), startDate.minusDays(1));
        
        // 执行预测
        String method = (String) parameters.getOrDefault("method", "arima");
        int forecastPeriod = endDate.compareTo(startDate);
        
        if ("arima".equals(method)) {
            int p = (int) parameters.getOrDefault("p", 1);
            int d = (int) parameters.getOrDefault("d", 1);
            int q = (int) parameters.getOrDefault("q", 1);
            return arimaForecast(historicalData, p, d, q, forecastPeriod);
        } else if ("holtWinters".equals(method)) {
            double alpha = (double) parameters.getOrDefault("alpha", 0.3);
            double beta = (double) parameters.getOrDefault("beta", 0.1);
            double gamma = (double) parameters.getOrDefault("gamma", 0.1);
            int seasonLength = (int) parameters.getOrDefault("seasonLength", 7);
            return holtWintersForecast(historicalData, alpha, beta, gamma, seasonLength, forecastPeriod);
        } else {
            throw new IllegalArgumentException("Unsupported forecast method: " + method);
        }
    }
}
```

### 步骤 7：创建预测结果服务

```java
// ForecastResultService.java
package com.example.domain.statistics.service.forecast;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 处理预测结果
 */
@Service
public class ForecastResultService {
    /**
     * 评估预测准确性
     * @param actualData 实际数据
     * @param forecastData 预测数据
     * @return 准确性评估指标
     */
    public Map<String, Double> evaluateAccuracy(Map<LocalDate, Double> actualData, Map<LocalDate, Double> forecastData) {
        // 准确性评估逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    /**
     * 可视化预测结果
     * @param actualData 实际数据
     * @param forecastData 预测数据
     * @return 可视化数据
     */
    public Map<String, Object> visualizeForecast(Map<LocalDate, Double> actualData, Map<LocalDate, Double> forecastData) {
        // 可视化逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    /**
     * 计算预测区间
     * @param forecastData 预测数据
     * @param confidenceLevel 置信水平
     * @return 预测区间
     */
    public Map<LocalDate, double[]> calculateForecastIntervals(Map<LocalDate, Double> forecastData, double confidenceLevel) {
        // 预测区间计算逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
}
```

### 步骤 8：创建预测导出服务

```java
// ForecastExportService.java
package com.example.domain.statistics.service.forecast;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 处理预测结果导出
 */
@Service
public class ForecastExportService {
    /**
     * 导出预测结果为CSV
     * @param forecastData 预测数据
     * @param filePath 文件路径
     */
    public void exportToCsv(Map<LocalDate, Double> forecastData, String filePath) {
        // CSV导出逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    /**
     * 导出预测结果为JSON
     * @param forecastData 预测数据
     * @param filePath 文件路径
     */
    public void exportToJson(Map<LocalDate, Double> forecastData, String filePath) {
        // JSON导出逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
    
    /**
     * 生成预测报告
     * @param forecastData 预测数据
     * @param actualData 实际数据
     * @param parameters 预测参数
     * @param filePath 文件路径
     */
    public void generateReport(Map<LocalDate, Double> forecastData, Map<LocalDate, Double> actualData, 
                          Map<String, Object> parameters, String filePath) {
        // 报告生成逻辑
        // 从原SalesForecastService中提取出来
        // ...
    }
}
```

### 步骤 9：创建统一的预测服务门面

```java
// SalesForecastServicePlus.java
package com.example.domain.statistics.service;

import com.example.domain.statistics.service.forecast.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 销售预测服务门面
 * 整合多个预测相关服务，提供统一的接口
 */
@Service
public class SalesForecastService {
    @Autowired
    private BasicForecastService basicForecastService;
    
    @Autowired
    private AdvancedForecastService advancedForecastService;
    
    @Autowired
    private ForecastDataService dataService;
    
    @Autowired
    private ForecastResultService resultService;
    
    @Autowired
    private ForecastExportService exportService;
    
    /**
     * 执行基本预测
     * @param productId 产品ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param method 预测方法
     * @param parameters 预测参数
     * @return 预测结果
     */
    public Map<LocalDate, Double> executeBasicForecast(Long productId, LocalDate startDate, LocalDate endDate, 
                                             String method, Map<String, Object> parameters) {
        parameters.put("method", method);
        return basicForecastService.forecast(productId, startDate, endDate, parameters);
    }
    
    /**
     * 执行高级预测
     * @param productId 产品ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param method 预测方法
     * @param parameters 预测参数
     * @return 预测结果
     */
    public Map<LocalDate, Double> executeAdvancedForecast(Long productId, LocalDate startDate, LocalDate endDate, 
                                               String method, Map<String, Object> parameters) {
        parameters.put("method", method);
        return advancedForecastService.forecast(productId, startDate, endDate, parameters);
    }
    
    /**
     * 生成完整的预测报告
     * @param productId 产品ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param method 预测方法
     * @param parameters 预测参数
     * @param exportPath 导出路径
     */
    public void generateForecastReport(Long productId, LocalDate startDate, LocalDate endDate, 
                               String method, Map<String, Object> parameters, String exportPath) {
        // 获取历史数据用于比较
        Map<LocalDate, Double> historicalData = dataService.getHistoricalSalesData(
            productId, startDate.minusMonths(1), startDate.minusDays(1));
        
        // 执行预测
        Map<LocalDate, Double> forecastData;
        if (isBasicMethod(method)) {
            forecastData = executeBasicForecast(productId, startDate, endDate, method, parameters);
        } else {
            forecastData = executeAdvancedForecast(productId, startDate, endDate, method, parameters);
        }
        
        // 生成报告
        exportService.generateReport(forecastData, historicalData, parameters, exportPath);
    }
    
    /**
     * 判断是否为基本预测方法
     * @param method 预测方法
     * @return 是否为基本方法
     */
    private boolean isBasicMethod(String method) {
        return "movingAverage".equals(method) || "exponentialSmoothing".equals(method);
    }
}
```

## 重构的好处

1. **单一职责原则**：每个类都有明确的单一职责，更容易理解和维护。

2. **开放封闭原则**：容易扩展新的预测算法，只需实现`ForecastService`接口。

3. **可测试性**：每个小型服务更容易单独测试。

4. **代码复用**：各个功能组件可以独立复用，例如数据处理部分可以被其他统计功能使用。

5. **团队协作**：多人可以并行开发不同的服务类，减少代码冲突。

## 拆分后的目录结构

```
src/main/java/com/example/domain/statistics/
├── controller/
│   └── StatisticsController.java
├── service/
│   ├── SalesForecastService.java (门面类)
│   └── forecast/                  (预测相关服务)
│       ├── ForecastService.java       (接口)
│       ├── BasicForecastService.java  (基本预测算法)
│       ├── AdvancedForecastService.java (高级预测算法)
│       ├── ForecastDataService.java   (数据准备)
│       ├── TimeSeriesAnalysisService.java (时间序列分析)
│       ├── ForecastResultService.java (结果处理)
│       └── ForecastExportService.java (导出服务)
├── repository/
├── entity/
└── dto/
```

按照这种方式，可以将过大的服务类拆分为多个更小、更专注的服务类，使代码更易于维护和理解。 