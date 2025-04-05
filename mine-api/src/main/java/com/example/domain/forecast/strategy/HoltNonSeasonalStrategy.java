package com.example.domain.forecast.strategy;

import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Holt 双指数平滑 (非季节性) 预测策略。
 * 适用于历史数据量达到一定阈值（例如 90 天）但不足以应用季节性模型，或不需要考虑季节性的情况。
 */
@Component // 注册为 Spring Bean
@Slf4j
public class HoltNonSeasonalStrategy implements ForecastStrategy {


    // 定义该策略适用的数据长度范围
    private static final int MIN_DATA_LENGTH = 10;


    @Override
    public double[] forecast(double[] historicalData, int forecastDays) throws MyException {
        try {
            // 1. 参数优化 (使用平滑后的数据)
            double[] optimalParams = new double[]{0.7, 0.1}; // 固定alpha=0.7，beta=0.1

            // 2. 执行预测 (使用平滑后的数据)
            double[] forecastResult = forecastHoltNonSeasonalInternal( // 调用内部迁移的方法
                    historicalData, // 使用平滑后的数据进行预测
                    forecastDays, optimalParams[0], // alpha
                    optimalParams[1]  // beta
            );

            // 添加日志：记录预测结果数组
            log.debug("Holt Non-Seasonal 预测完成，预测结果数组: {}", Arrays.toString(forecastResult));
            // 计算训练集上的拟合误差（MAE）
            double[] fittedTrain = forecastHoltNonSeasonalInternal(historicalData, historicalData.length, optimalParams[0], optimalParams[1]);
            double mae = calculateMAEInternal(historicalData, fittedTrain);
            log.info("Holt Non-Seasonal 模型在训练集上的MAE误差为: {}", mae);
            return forecastResult;

        } catch (Exception e) {
            log.error("Holt Non-Seasonal 策略执行失败: {}", e.getMessage(), e);
            throw new MyException("Holt Non-Seasonal 预测失败: " + e.getMessage());
        }
    }

    @Override
    public boolean canHandle(int dataLength) {
        // 适用于 90 天及以上的数据
        return dataLength >= MIN_DATA_LENGTH;
    }

    @Override
    public String getStrategyName() {
        return "Holt Non-Seasonal";
    }

    @Override
    public int getMinDataLength() {
        // 返回该策略适用的最小数据长度，用于排序
        return MIN_DATA_LENGTH;
    }



    /**
     * [内部方法] 使用双指数平滑(Holt)方法预测(无季节性)，使用指定参数。
     */
    private static double[] forecastHoltNonSeasonalInternal(double[] data, int periods, double alpha, double beta) {

        if (periods <= 0) {
            return new double[0];
        }

        double level = data[0];
        double trend = calculateInitialTrendInternal(data);

        for (int i = 1; i < data.length; i++) {
            double prevLevel = level;
            level = alpha * data[i] + (1 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1 - beta) * trend;
        }

        double[] forecast = new double[periods];
        for (int i = 1; i <= periods; i++) {
            double forecastValue = level + i * trend;
            forecast[i - 1] = Math.max(0, forecastValue);
        }
        return forecast;
    }



    /**
     * [内部方法] 计算初始趋势。
     */
    private static double calculateInitialTrendInternal(double[] data) {
        if (data.length < 2) return 0;
        int n = Math.min(data.length, 5);
        if (n < 2) return data.length > 1 ? data[1] - data[0] : 0;
        double sumDiff = 0;
        for (int i = 1; i < n; i++) {
            sumDiff += (data[i] - data[i - 1]);
        }
        return sumDiff / (n - 1);
    }

    /**
     * [内部方法] 使用时间序列交叉验证为 Holt 非季节性模型找到最优参数 (alpha, beta)。
     */

    /**
     * [内部方法] 计算 MAE。
     */
    private static double calculateMAEInternal(double[] actual, double[] forecast) {
        if (actual == null || forecast == null || actual.length != forecast.length) {
            throw new IllegalArgumentException("实际值和预测值数组必须非空且长度相同");
        }
        if (actual.length == 0) return Double.NaN;
        double sumAbsError = 0;
        for (int i = 0; i < actual.length; i++) {
            sumAbsError += Math.abs(actual[i] - forecast[i]);
        }
        return sumAbsError / actual.length;
    }

}
