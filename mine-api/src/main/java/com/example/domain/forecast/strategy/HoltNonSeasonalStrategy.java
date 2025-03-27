package com.example.domain.forecast.strategy;

import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Holt 双指数平滑 (非季节性) 预测策略。
 * 适用于历史数据量达到一定阈值（例如 90 天）但不足以应用季节性模型，或不需要考虑季节性的情况。
 */
@Component // 注册为 Spring Bean
@Slf4j
public class HoltNonSeasonalStrategy implements ForecastStrategy {

    // 定义该策略适用的数据长度范围
    private static final int MIN_DATA_LENGTH = 90; // 至少需要约 3 个月数据
    // private static final int MAX_DATA_LENGTH = HoltWintersForecast.DEFAULT_DAILY_SEASON_LENGTH; // 移除上限
    private static final int OPTIMIZATION_MIN_DATA_LENGTH = 30; // 参数优化至少需要约 1 个月数据 (比应用阈值低)

    @Override
    public double[] forecast(double[] historicalData, int forecastDays) throws MyException {
        log.debug("执行 Holt Non-Seasonal 策略，原始数据: {}, 预测天数: {}", historicalData, forecastDays);
        historicalData=Arrays.copyOfRange(historicalData,historicalData.length-90,historicalData.length);
        try {
            // 0. 数据预处理：应用 7 日移动平均
            int movingAverageWindow = 7;
            double[] smoothedData;
            if (historicalData.length >= movingAverageWindow) {
                 log.debug("应用 {} 日移动平均进行预处理...", movingAverageWindow);
                 smoothedData = applySimpleMovingAverageInternal(historicalData, movingAverageWindow);
            } else {
                 log.warn("数据量 ({}) 不足 {} 天，跳过移动平均预处理，使用原始数据。", historicalData.length, movingAverageWindow);
                 smoothedData = historicalData; // 使用原始数据
            }
            log.debug("预处理后数据长度: {}", smoothedData.length);


            // 1. 参数优化 (使用平滑后的数据)
            double[] optimalParams;
             // 注意：参数优化的最小数据量检查应该基于平滑后的数据长度
             if (smoothedData.length >= OPTIMIZATION_MIN_DATA_LENGTH) {
                 log.debug("数据量充足 ({}), 执行 Holt 非季节性参数优化...", smoothedData.length);
                 // 使用 MAE 进行评估
                 optimalParams = findOptimalHoltParametersCVInternal( // 调用内部迁移的方法
                         smoothedData, null, null, // 使用默认参数范围搜索
                         true // useMAE = true
                 );
             } else {
                 log.warn("数据量 ({}) 不足，无法执行 Holt 非季节性参数优化，将使用默认参数。", smoothedData.length);
                 // 提供一组相对稳健的默认参数
                 optimalParams = new double[]{0.2, 0.1}; // 示例默认值 {alpha, beta}
             }
             log.debug("使用的 Holt 非季节性参数: alpha={}, beta={}", optimalParams[0], optimalParams[1]);

            // 添加日志：记录用于预测的平滑后数据
            log.debug("用于 Holt 预测的平滑后数据: {}", Arrays.toString(smoothedData));

            // 2. 执行预测 (使用平滑后的数据)
            double[] forecastResult = forecastHoltNonSeasonalInternal( // 调用内部迁移的方法
                    smoothedData, // 使用平滑后的数据进行预测
                    forecastDays,
                    optimalParams[0], // alpha
                    optimalParams[1]  // beta
            );

            // 添加日志：记录预测结果数组
            log.debug("Holt Non-Seasonal 预测完成，预测结果数组: {}", Arrays.toString(forecastResult));
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

    // #########################################################################
    // # Migrated Methods from HoltWintersForecast & Utility Methods
    // #########################################################################

     /**
     * [内部方法] 对原始数据应用简单的移动平均平滑处理。
     */
    private static double[] applySimpleMovingAverageInternal(double[] rawData, int windowSize) {
        if (rawData == null || rawData.length == 0) {
            return new double[0];
        }
        if (windowSize <= 0) {
            throw new IllegalArgumentException("窗口大小必须为正数");
        }
        if (windowSize == 1) {
            return Arrays.copyOf(rawData, rawData.length); // 无需平滑
        }

        double[] smoothedData = new double[rawData.length];
        double currentSum = 0;
        int count = 0;

        for (int i = 0; i < rawData.length; i++) {
            currentSum += rawData[i];
            count++;

            if (i >= windowSize) {
                currentSum -= rawData[i - windowSize]; // 移除窗口外的最旧数据
                count--; // 理论上 count 应该等于 windowSize，但这样写更安全
            }

            // 对窗口内数据计算平均值
            smoothedData[i] = currentSum / count;
        }

        return smoothedData;
    }

     /**
     * [内部方法] 使用双指数平滑(Holt)方法预测(无季节性)，使用指定参数。
     */
    private static double[] forecastHoltNonSeasonalInternal(double[] data, int periods, double alpha, double beta) {
        if (data == null || data.length < 2) {
            throw new IllegalArgumentException("Holt 非季节性预测至少需要 2 个数据点");
        }
        validateNonSeasonalParametersInternal(alpha, beta);
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
     * [内部方法] 验证非季节性模型参数 (alpha, beta)。
     */
    private static void validateNonSeasonalParametersInternal(double alpha, double beta) {
        if (alpha <= 0 || alpha >= 1) throw new IllegalArgumentException("Alpha 必须在 (0, 1) 之间");
        if (beta < 0 || beta >= 1) throw new IllegalArgumentException("Beta 必须在 [0, 1) 之间");
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
            sumDiff += (data[i] - data[i-1]);
        }
        return sumDiff / (n - 1);
    }

    /**
     * [内部方法] 使用时间序列交叉验证为 Holt 非季节性模型找到最优参数 (alpha, beta)。
     */
    private static double[] findOptimalHoltParametersCVInternal(double[] trainingData,
                                                                double[] alphaValues,
                                                                double[] betaValues,
                                                                boolean useMAE) {
        if (trainingData == null || trainingData.length < 15) {
            throw new IllegalArgumentException("Holt 参数优化需要至少 15 个数据点");
        }

        int minTrainSize = Math.max(10, trainingData.length / 3);
        int validationWindowSize = Math.max(5, trainingData.length / 5);
        int step = Math.max(1, validationWindowSize / 2);
        int numFolds = Math.max(3, (trainingData.length - minTrainSize - validationWindowSize) / step + 1);
        numFolds = Math.min(numFolds, 10);

        log.info("开始 Holt 非季节性参数优化 ({} 折交叉验证, 评估指标: {})...", numFolds, useMAE ? "MAE" : "MAPE");

        double bestAvgError = Double.MAX_VALUE;
        double[] bestParams = new double[2];

        alphaValues = (alphaValues == null || alphaValues.length == 0) ? IntStream.rangeClosed(1, 50).mapToDouble(i -> i * 0.01).toArray() : alphaValues;
        betaValues = (betaValues == null || betaValues.length == 0) ? IntStream.rangeClosed(0, 50).mapToDouble(i -> i * 0.01).toArray() : betaValues;

        for (double alpha : alphaValues) {
            for (double beta : betaValues) {
                try {
                    validateNonSeasonalParametersInternal(alpha, beta);
                    double totalError = 0;
                    int validFolds = 0;

                    for (int fold = 0; fold < numFolds; fold++) {
                        int validationStart = minTrainSize + fold * step;
                        int validationEnd = Math.min(validationStart + validationWindowSize, trainingData.length);
                        if (validationEnd - validationStart < 3) continue;

                        double[] foldTrainSet = Arrays.copyOfRange(trainingData, 0, validationStart);
                        double[] foldValidationSet = Arrays.copyOfRange(trainingData, validationStart, validationEnd);
                        double[] forecast = forecastHoltNonSeasonalInternal(foldTrainSet, foldValidationSet.length, alpha, beta);
                        double error = useMAE ? calculateMAEInternal(foldValidationSet, forecast) : calculateMAPEInternal(foldValidationSet, forecast);

                        if (!Double.isNaN(error) && Double.isFinite(error)) {
                            totalError += error;
                            validFolds++;
                        }
                    }

                    if (validFolds >= numFolds / 2) {
                        double avgError = totalError / validFolds;
                        if (avgError < bestAvgError) {
                            bestAvgError = avgError;
                            bestParams[0] = alpha;
                            bestParams[1] = beta;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Ignore invalid params
                } catch (Exception e) {
                    log.warn("Holt 参数优化中出现意外错误 (alpha={}, beta={}): {}", alpha, beta, e.getMessage());
                }
            }
        }

         if (bestAvgError == Double.MAX_VALUE) {
            log.warn("未能找到有效的 Holt 参数组合，将使用默认值或引发错误。");
             throw new RuntimeException("未能找到有效的 Holt 参数组合");
        }

        log.info("Holt 非季节性参数优化完成。最优参数: alpha={}, beta={}, 平均误差({}): {}",
                bestParams[0], bestParams[1], useMAE ? "MAE" : "MAPE", bestAvgError);
        return bestParams;
    }

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

    /**
     * [内部方法] 计算 MAPE。
     */
    private static double calculateMAPEInternal(double[] actual, double[] forecast) {
        if (actual == null || forecast == null || actual.length != forecast.length) {
            throw new IllegalArgumentException("实际值和预测值数组必须非空且长度相同");
        }
        if (actual.length == 0) return Double.NaN;
        double sumPercentageError = 0;
        int count = 0;
        for (int i = 0; i < actual.length; i++) {
            if (Math.abs(actual[i]) > 1e-6) {
                sumPercentageError += Math.abs((actual[i] - forecast[i]) / actual[i]);
                count++;
            }
        }
        return (count > 0) ? (sumPercentageError / count) * 100.0 : Double.NaN;
    }
}
