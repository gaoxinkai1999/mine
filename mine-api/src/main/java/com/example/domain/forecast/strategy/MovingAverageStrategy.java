package com.example.domain.forecast.strategy;

import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class MovingAverageStrategy implements ForecastStrategy {
    private static final int MIN_DATA_LENGTH = 30;
    private static final int MAX_DATA_LENGTH = 90;
    private static final int DEFAULT_WINDOW_SIZE = 7;

    @Override
    public double[] forecast(double[] historicalData, int forecastDays) throws MyException {
        log.debug("执行 Moving Average 策略，数据长度: {}, 预测天数: {}, 窗口大小: {}",
                historicalData.length, forecastDays, DEFAULT_WINDOW_SIZE);

        if (historicalData.length < DEFAULT_WINDOW_SIZE) {
            log.warn("数据量 ({}) 小于移动平均窗口大小 ({}))", 
                    historicalData.length, DEFAULT_WINDOW_SIZE);
            double average = Arrays.stream(historicalData).average().orElse(0.0);
            double[] forecastResult = new double[forecastDays];
            Arrays.fill(forecastResult, Math.max(0, average));
            return forecastResult;
        }

        try {
            double[] movingAverages = calculateMovingAverage(historicalData, DEFAULT_WINDOW_SIZE);
            double lastMovingAverage = movingAverages[movingAverages.length - 1];
            
            double[] forecastResult = new double[forecastDays];
            Arrays.fill(forecastResult, Math.max(0, lastMovingAverage));
            return forecastResult;
        } catch (Exception e) {
            log.error("策略执行失败", e);
            throw new MyException("预测失败: " + e.getMessage());
        }
    }

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
        return dataLength >= MIN_DATA_LENGTH && dataLength < MAX_DATA_LENGTH;
    }

    @Override
    public String getStrategyName() {
        return "Moving Average";
    }

    @Override
    public int getMinDataLength() {
        return MIN_DATA_LENGTH;
    }
}
