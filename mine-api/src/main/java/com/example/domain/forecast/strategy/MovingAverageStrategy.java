package com.example.domain.forecast.strategy;

import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

// @Component
@Component
@Slf4j
public class MovingAverageStrategy implements ForecastStrategy {
    private static final int MIN_DATA_LENGTH = 7;
    private static final int MAX_DATA_LENGTH = Integer.MAX_VALUE;
    
    // 调整为自适应窗口大小
    private static final int DEFAULT_WINDOW_SIZE = 7;
    private static final int MIN_WINDOW_SIZE = 3;
    private static final double WEIGHT_DECAY = 0.9; // 指数加权移动平均的衰减系数

    @Override
    public double[] forecast(double[] historicalData, int forecastDays) throws MyException {
        // 数据长度校验
        if (historicalData == null || historicalData.length == 0) {
            log.warn("历史数据为空，无法进行预测");
            return new double[forecastDays];
        }
        
        // 选择最合适的窗口大小和算法
        int windowSize = Math.min(DEFAULT_WINDOW_SIZE, historicalData.length);
        windowSize = Math.max(MIN_WINDOW_SIZE, windowSize); // 确保至少有3个点
        
        log.debug("执行改进的移动平均策略，数据长度: {}, 预测天数: {}, 窗口大小: {}",
                historicalData.length, forecastDays, windowSize);

        try {
            // 使用最近趋势进行预测
            double[] result;
            
            if (historicalData.length >= 14) {
                // 数据足够多时，使用指数加权移动平均
                log.info("数据点数 >= 14，使用指数加权移动平均 (EWMA)");
                result = forecastWithEWMA(historicalData, forecastDays, WEIGHT_DECAY);
            } else {
                // 数据较少时，使用简单移动平均并考虑趋势
                log.info("数据点数 < 14，使用简单移动平均加趋势");
                result = forecastWithSMAAndTrend(historicalData, forecastDays, windowSize);
            }
            
            // 固定窗口大小下的时间序列交叉验证评估
            evaluateUsingCV(historicalData, windowSize);

            return result;
        } catch (Exception e) {
            log.error("移动平均策略预测失败", e);
            // 出错时使用简单的平均值作为后备方案
            double average = Arrays.stream(historicalData).average().orElse(0.0);
            double[] forecastResult = new double[forecastDays];
            Arrays.fill(forecastResult, Math.max(0, average));
            return forecastResult;
        }
    }
    
    /**
     * 使用指数加权移动平均进行预测
     */
    private double[] forecastWithEWMA(double[] data, int forecastDays, double alpha) {
        // 计算最新的EWMA值
        double ewma = data[0];
        for (int i = 1; i < data.length; i++) {
            ewma = alpha * data[i] + (1 - alpha) * ewma;
        }
        
        // 计算最近的趋势
        double trend = 0;
        if (data.length >= 3) {
            // 使用最后几个点估计趋势
            int n = Math.min(5, data.length);
            double[] lastNPoints = Arrays.copyOfRange(data, data.length - n, data.length);
            trend = estimateTrend(lastNPoints);
        }
        
        double[] forecast = new double[forecastDays];
        for (int i = 0; i < forecastDays; i++) {
            // 使用EWMA值加简单趋势估计进行预测
            forecast[i] = Math.max(0, ewma + trend * (i + 1));
        }
        
        return forecast;
    }
    
    /**
     * 使用简单移动平均加趋势进行预测
     */
    private double[] forecastWithSMAAndTrend(double[] data, int forecastDays, int windowSize) {
        // 计算最新的移动平均值
        double[] movingAverages = calculateMovingAverage(data, windowSize);
        double lastMA = movingAverages[movingAverages.length - 1];
        
        // 估计趋势
        double trend = 0;
        if (data.length >= 3) {
            trend = estimateTrend(data);
        }
        
        double[] forecast = new double[forecastDays];
        for (int i = 0; i < forecastDays; i++) {
            // 添加趋势并确保非负
            forecast[i] = Math.max(0, lastMA + trend * (i + 1));
        }
        
        return forecast;
    }
    
    /**
     * 估计数据的线性趋势（每单位时间的平均变化率）
     */
    private double estimateTrend(double[] data) {
        if (data.length < 2) return 0;
        
        // 使用最简单的方法估计趋势：最后一个点与第一个点之差除以时间跨度
        double firstValue = data[0];
        double lastValue = data[data.length - 1];
        double slope = (lastValue - firstValue) / (data.length - 1);
        
        // 简单平滑，避免过大的趋势
        if (Math.abs(slope) > 0.5 * Math.abs(lastValue)) {
            slope = 0.5 * Math.signum(slope) * Math.abs(lastValue);
        }
        
        return slope;
    }
    
    /**
     * 通过时间序列交叉验证评估预测性能
     */
    private void evaluateUsingCV(double[] historicalData, int windowSize) {
        double totalError = 0;
        int count = 0;
        for (int i = windowSize; i < historicalData.length; i++) {
            double[] train = Arrays.copyOfRange(historicalData, 0, i);
            double[] movingAvg = calculateMovingAverage(train, windowSize);
            double predicted = movingAvg[movingAvg.length -1];
            double actual = historicalData[i];
            totalError += Math.abs(actual - predicted);
            count++;
        }
        if (count > 0) {
            double cvMae = totalError / count;
            log.info("移动平均交叉验证MAE: {}", String.format("%.4f", cvMae));
        } else {
            log.info("数据量不足以进行移动平均交叉验证 (窗口大小{})", windowSize);
        }
    }

    /**
     * 计算简单移动平均
     */
    private double[] calculateMovingAverage(double[] data, int windowSize) {
        if (data == null || data.length == 0) return new double[0];
        if (windowSize <= 0) throw new IllegalArgumentException("无效窗口大小");

        double[] result = new double[data.length];
        double sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
            if (i >= windowSize) sum -= data[i - windowSize];
            result[i] = sum / Math.min(i + 1, windowSize);
        }
        return result;
    }

    @Override
    public boolean canHandle(int dataLength) {
        return dataLength >= MIN_DATA_LENGTH;
    }

    @Override
    public String getStrategyName() {
        return "Enhanced Moving Average";
    }

    @Override
    public int getMinDataLength() {
        return MIN_DATA_LENGTH;
    }
}
