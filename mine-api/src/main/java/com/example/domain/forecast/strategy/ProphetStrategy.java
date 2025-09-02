package com.example.domain.forecast.strategy;

import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prophet预测策略
 * 通过调用Python Prophet服务进行销量预测
 */
@Component
@Slf4j
public class ProphetStrategy implements ForecastStrategy {
    
    // Prophet至少需要21天的原始日数据（提高最小要求）
    private static final int MIN_RAW_DAILY_DATA_LENGTH = 21; 
    
    // 建议的理想数据长度（用于日志提示）
    private static final int RECOMMENDED_DATA_LENGTH = 30;
    
    @Value("${forecast.prophet.api.url:http://localhost:8000}")
    private String prophetApiUrl;
    
    // 是否默认启用高精度模式
    @Value("${forecast.prophet.high-accuracy:true}")
    private boolean defaultHighAccuracy;
    
    private final RestTemplate restTemplate;
    
    public ProphetStrategy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Override
    public double[] forecast(double[] historicalData, int forecastDays) throws MyException {
        try {
            // 记录数据长度信息
            if (historicalData.length < RECOMMENDED_DATA_LENGTH) {
                log.warn("Prophet预测的历史数据长度({})低于建议值({}天)，可能影响预测准确性", 
                        historicalData.length, RECOMMENDED_DATA_LENGTH);
            }
            
            // 1. 准备请求数据
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> dataPoints = new ArrayList<>();
            
            // 假设我们有日期信息，如果没有，需要从当前日期往前推算
            LocalDate endDate = LocalDate.now().minusDays(1); // 假设最后一个数据点是昨天
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (int i = 0; i < historicalData.length; i++) {
                LocalDate date = endDate.minusDays(historicalData.length - 1 - i);
                Map<String, Object> point = new HashMap<>();
                point.put("date", date.format(formatter));
                point.put("sales", historicalData[i]);
                dataPoints.add(point);
            }
            
            requestBody.put("historical_data", dataPoints);
            requestBody.put("forecast_days", forecastDays);
            
            // 启用高精度模式
            requestBody.put("accuracy_mode", defaultHighAccuracy);
            
            // 2. 设置Prophet参数（可选）
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("changepoint_prior_scale", 0.05);
            parameters.put("seasonality_mode", "multiplicative");
            parameters.put("add_weekly_seasonality", true);
            parameters.put("add_holidays", true);
            
            // 启用高精度模式
            parameters.put("high_accuracy", defaultHighAccuracy);
            
            // 添加更多参数
            if (defaultHighAccuracy) {
                // 高精度模式下，使用更多的变化点和MCMC采样
                parameters.put("mcmc_samples", 100);  // 启用MCMC采样
                parameters.put("uncertainty_samples", 2000);  // 增加样本数量
            }
            
            requestBody.put("parameters", parameters);
            
            // 3. 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("发送预测请求到Prophet服务: {}, 数据点数: {}, 预测天数: {}, 高精度模式: {}", 
                    prophetApiUrl + "/api/forecast", 
                    historicalData.length, 
                    forecastDays,
                    defaultHighAccuracy);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    prophetApiUrl + "/api/forecast", 
                    request, 
                    Map.class
            );
            
            // 4. 处理响应
            if (response == null) {
                throw new MyException("Prophet API返回空响应");
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> forecastResults = (List<Map<String, Object>>) response.get("forecast");
            if (forecastResults == null || forecastResults.isEmpty()) {
                throw new MyException("Prophet API返回的预测结果为空");
            }
            
            double[] result = new double[forecastDays];
            
            for (int i = 0; i < forecastResults.size() && i < forecastDays; i++) {
                result[i] = ((Number) forecastResults.get(i).get("sales")).doubleValue();
            }
            
            // 5. 记录预测指标（如果有）
            if (response.containsKey("model_metrics")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metrics = (Map<String, Object>) response.get("model_metrics");
                if (metrics != null && !metrics.isEmpty()) {
                    log.info("Prophet预测指标 - MAPE: {}, RMSE: {}, MAE: {}", 
                            metrics.get("mape"), 
                            metrics.get("rmse"),
                            metrics.get("mae"));
                }
            }
            
            // 记录执行时间（如果API返回）
            if (response.containsKey("execution_time_ms")) {
                log.debug("Prophet预测执行时间: {}ms", response.get("execution_time_ms"));
            }
            
            log.info("Prophet预测完成，返回{}天预测结果", result.length);
            return result;
            
        } catch (Exception e) {
            log.error("Prophet预测失败: {}", e.getMessage(), e);
            throw new MyException("Prophet预测失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canHandle(int rawDataLength) { // 参数名修改为 rawDataLength
        // 比较的是 原始日数据长度 >= Prophet要求的最小日数据长度
        return rawDataLength >= MIN_RAW_DAILY_DATA_LENGTH; 
    }
    
    @Override
    public String getStrategyName() {
        return "Prophet (External API)";
    }
    
    @Override
    public int getMinDataLength() {
        // 返回的是Prophet实际需要的最小原始日数据天数
        return MIN_RAW_DAILY_DATA_LENGTH; 
    }
}
