package com.example.domain.forecast.strategy;

import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

// @Component
@Slf4j
public class MovingAverageStrategy implements ForecastStrategy {
    private static final int MIN_DATA_LENGTH = 2;
    private static final int MAX_DATA_LENGTH = Integer.MAX_VALUE;
    private static final int DEFAULT_WINDOW_SIZE = 7;  // 固定窗口大小

    @Override
    public double[] forecast(double[] historicalData, int forecastDays) throws MyException {
        log.debug("执行 Moving Average 策略，数据长度: {}, 预测天数: {}, 窗口大小: {}",
                historicalData.length, forecastDays, DEFAULT_WINDOW_SIZE);

        if (historicalData.length == 0) {
            log.warn("历史数据为空");
            return new double[forecastDays];
        }

        // 固定窗口大小下的时间序列交叉验证评估
        double totalError = 0;
        int count = 0;
        for (int i = DEFAULT_WINDOW_SIZE; i < historicalData.length; i++) {
            double[] train = Arrays.copyOfRange(historicalData, 0, i);
            double[] movingAvg = calculateMovingAverage(train, DEFAULT_WINDOW_SIZE);
            double predicted = movingAvg[movingAvg.length -1];
            double actual = historicalData[i];
            totalError += Math.abs(actual - predicted);
            count++;
        }
        if (count > 0) {
            double cvMae = totalError / count;
            log.info("固定窗口大小={}的交叉验证MAE: {}", DEFAULT_WINDOW_SIZE, String.format("%.4f", cvMae));
        } else {
            log.info("数据量不足以进行滑动平均交叉验证 (窗口大小{})", DEFAULT_WINDOW_SIZE);
        }

        double[] movingAverages;
        double lastMovingAverage;

        try {
            movingAverages = calculateMovingAverage(historicalData, DEFAULT_WINDOW_SIZE);
            lastMovingAverage = movingAverages[movingAverages.length - 1];
        } catch (Exception e) {
            log.error("计算移动平均失败，使用历史均值", e);
            double average = Arrays.stream(historicalData).average().orElse(0.0);
            lastMovingAverage = average;
        }

        double[] forecastResult = new double[forecastDays];
        Arrays.fill(forecastResult, Math.max(0, lastMovingAverage));

        return forecastResult;
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
        return dataLength >= MIN_DATA_LENGTH;
    }

    @Override
    public String getStrategyName() {
        return "Moving Average (Fixed Window CV Eval)";
    }

    @Override
    public int getMinDataLength() {
        return MIN_DATA_LENGTH;
    }
}
