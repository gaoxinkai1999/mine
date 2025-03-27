package com.example.domain.forecast.strategy;

import com.example.HoltWintersForecast; // 复用其中的移动平均工具方法
import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 简单移动平均 (SMA) 预测策略。
 * 适用于历史数据较短，趋势和季节性不明显的情况（例如 1 个月到 3 个月的数据）。
 * 预测逻辑：使用最后计算出的移动平均值作为未来所有日期的预测值。
 */
@Component // 注册为 Spring Bean
@Slf4j
public class MovingAverageStrategy implements ForecastStrategy {

    // 定义该策略适用的数据长度范围
    private static final int MIN_DATA_LENGTH = 30; // 至少需要约 1 个月数据
    private static final int MAX_DATA_LENGTH = 90; // 处理 30 到 89 天的数据
    private static final int DEFAULT_WINDOW_SIZE = 30; // 默认移动平均窗口大小（例如 7 天）

    @Override
    public double[] forecast(double[] historicalData, int forecastDays) throws MyException {
        log.debug("执行 Moving Average 策略，数据长度: {}, 预测天数: {}, 窗口大小: {}",
                historicalData.length, forecastDays, DEFAULT_WINDOW_SIZE);

        if (historicalData.length < DEFAULT_WINDOW_SIZE) {
            // 如果数据量甚至小于窗口大小，则使用所有数据的平均值
            log.warn("数据量 ({}) 小于移动平均窗口大小 ({})，将使用整体平均值进行预测。",
                     historicalData.length, DEFAULT_WINDOW_SIZE);
            double average = Arrays.stream(historicalData).average().orElse(0.0);
            double[] forecastResult = new double[forecastDays];
            Arrays.fill(forecastResult, Math.max(0, average)); // 填充预测结果并确保非负
            log.debug("Moving Average (整体平均) 预测完成，结果: {}", Arrays.toString(forecastResult));
            return forecastResult;
        }

        try {
            // 1. 计算历史数据的移动平均值
            // 复用 HoltWintersForecast 类中的静态方法
            double[] movingAverages = HoltWintersForecast.applySimpleMovingAverage(historicalData, DEFAULT_WINDOW_SIZE);

            // 2. 获取最后一个移动平均值
            // 注意：applySimpleMovingAverage 返回的数组与输入等长，最后一个有效值在末尾
            double lastMovingAverage = movingAverages[movingAverages.length - 1];

            // 3. 使用最后一个移动平均值填充未来预测
            double[] forecastResult = new double[forecastDays];
            Arrays.fill(forecastResult, Math.max(0, lastMovingAverage)); // 确保预测值非负

            log.debug("Moving Average 预测完成，最后一个平均值: {}, 结果: {}", lastMovingAverage, Arrays.toString(forecastResult));
            return forecastResult;

        } catch (Exception e) {
            log.error("Moving Average 策略执行失败: {}", e.getMessage(), e);
            throw new MyException("Moving Average 预测失败: " + e.getMessage());
        }
    }

    @Override
    public boolean canHandle(int dataLength) {
        // 适用于 30 到 89 天的数据
        return dataLength >= MIN_DATA_LENGTH && dataLength < MAX_DATA_LENGTH;
    }

    @Override
    public String getStrategyName() {
        return "Moving Average";
    }

    @Override
    public int getMinDataLength() {
        // 返回该策略适用的最小数据长度，用于排序
        return MIN_DATA_LENGTH;
    }
}
