package com.example.domain.forecast.strategy;

import com.example.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Holt 双指数平滑 (非季节性) 预测策略。
 * 包含参数优化功能，基于时间序列交叉验证和网格搜索寻找最佳 alpha 和 beta。
 */
 @Component // 临时禁用HoltNonSeasonalStrategy */
@Slf4j
public class HoltNonSeasonalStrategy implements ForecastStrategy {

    // 定义该策略适用的最小数据长度
    private static final int MIN_DATA_LENGTH = 10; // 至少需要10个点来做基本的 Holt 和 CV

    // --- 参数优化相关常量 ---
    // 网格搜索的范围和步长 (可以根据需要调整)
    private static final double[] ALPHA_GRID;
    private static final double[] BETA_GRID;

    static {
        ALPHA_GRID = generateGrid(0.5, 1.0, 0.01);
        BETA_GRID = generateGrid(0.1, 0.5, 0.01);
    }

    private static double[] generateGrid(double start, double end, double step) {
        int size = (int) Math.round((end - start) / step) + 1;
        double[] grid = new double[size];
        for (int i = 0; i < size; i++) {
            grid[i] = start + i * step;
        }
        return grid;
    }
    // 时间序列交叉验证中，用于初始训练的最小数据点数量
    private static final int CV_INITIAL_TRAIN_SIZE = Math.max(5, MIN_DATA_LENGTH / 2); // 至少5个点，或者最小要求的一半
    // 默认参数，在优化失败或数据不足时使用
    private static final double DEFAULT_ALPHA = 0.7;
    private static final double DEFAULT_BETA = 0.1;

    @Override
    public double[] forecast(double[] historicalData, int forecastDays) throws MyException {
        if (historicalData == null || historicalData.length == 0) {
            log.warn("历史数据为空，无法进行 Holt 预测。");
            return new double[forecastDays]; // 返回全零预测
        }

        // 检查数据长度是否满足最小要求
        if (!canHandle(historicalData.length)) {
            log.warn("数据长度 ({}) 不足 {}，无法应用 Holt 策略。返回全零预测。", historicalData.length, MIN_DATA_LENGTH);
            return new double[forecastDays];
        }

        try {
            // 1. 参数优化 (使用时间序列交叉验证)
            double[] optimalParams={0.5,0.1};
            // double[] optimalParams = optimizeParameters(historicalData);
            double optimalAlpha = optimalParams[0];
            double optimalBeta = optimalParams[1];
            log.info("商品数据长度: {}, 优化后的 Holt 参数: alpha={}, beta={}", historicalData.length, String.format("%.3f", optimalAlpha), String.format("%.3f", optimalBeta));

            // 2. 使用优化后的参数执行最终预测
            double[] forecastResult = forecastHoltNonSeasonalInternal(
                    historicalData, // 使用完整的历史数据进行最终预测
                    forecastDays,
                    optimalAlpha,
                    optimalBeta
            );

            // 可选：计算并记录优化后模型在 *整个* 训练集上的拟合误差 (注意：这是样本内误差)
            double[] fittedTrain = forecastHoltNonSeasonalInternal(historicalData, historicalData.length, optimalAlpha, optimalBeta);
            double maeInSample = calculateMAEInternal(historicalData, fittedTrain);
            log.info("Holt Non-Seasonal (Optimized) 模型在训练集上的拟合 MAE: {}", String.format("%.4f", maeInSample));

            log.debug("Holt Non-Seasonal (Optimized) 预测完成，预测天数: {}, 结果: {}", forecastDays, Arrays.toString(forecastResult));
            return forecastResult;

        } catch (Exception e) {
            log.error("Holt Non-Seasonal 策略执行失败 (数据长度 {}): {}", historicalData.length, e.getMessage(), e);
            // 在失败时可以考虑返回一个默认预测或重新抛出异常
            // 这里选择抛出，让上层处理
            throw new MyException("Holt Non-Seasonal 预测失败: " + e.getMessage());
        }
    }

    /**
     * 使用网格搜索和时间序列交叉验证（滚动预测原点）优化 Holt 模型的 alpha 和 beta 参数。
     *
     * @param data 历史时间序列数据。
     * @return 包含最优 [alpha, beta] 的数组。如果优化失败，则返回默认值。
     */
    private double[] optimizeParameters(double[] data) {
        // 如果数据量不足以进行有意义的交叉验证，则直接返回默认参数
        if (data.length < CV_INITIAL_TRAIN_SIZE + 1) { // 需要至少 initial_size 用于训练，1个点用于测试
            log.warn("数据长度 ({}) 不足以进行有效的参数优化 (需要至少 {}), 使用默认参数 alpha={}, beta={}",
                    data.length, CV_INITIAL_TRAIN_SIZE + 1, DEFAULT_ALPHA, DEFAULT_BETA);
            return new double[]{DEFAULT_ALPHA, DEFAULT_BETA};
        }

        double bestAlpha = DEFAULT_ALPHA;
        double bestBeta = DEFAULT_BETA;
        double minMae = Double.POSITIVE_INFINITY;

        log.debug("开始 Holt 参数优化 (Grid Search + Rolling Forecast CV)... 数据长度: {}", data.length);

        // 遍历参数网格
        for (double alpha : ALPHA_GRID) {
            for (double beta : BETA_GRID) {
                // 执行时间序列交叉验证 (滚动预测原点, 预测步长 h=1)
                double totalAbsoluteError = 0;
                int cvCount = 0; // 有效的 CV 预测次数

                // 滚动原点：从 initial_size 开始，每次增加一个数据点进行训练，预测下一个点
                for (int i = CV_INITIAL_TRAIN_SIZE; i < data.length; i++) {
                    // 当前 CV 步骤的训练数据: data[0...i-1]
                    double[] trainCvData = Arrays.copyOfRange(data, 0, i);
                    // 期望预测的目标值: data[i]
                    double actualValue = data[i];

                    try {
                        // 使用当前 alpha, beta 对训练数据进行建模，并预测下一个点 (h=1)
                        double[] forecastCv = forecastHoltNonSeasonalInternal(trainCvData, 1, alpha, beta);

                        if (forecastCv != null && forecastCv.length > 0) {
                            double predictedValue = forecastCv[0];
                            totalAbsoluteError += Math.abs(actualValue - predictedValue);
                            cvCount++;
                        } else {
                            log.trace("CV 步骤 (alpha={}, beta={}, trainSize={}) 未产生有效预测", alpha, beta, i);
                        }
                    } catch (Exception e) {
                        // 记录 CV 中单步的错误，但继续尝试其他参数或步骤
                        log.warn("CV 步骤异常 (alpha={}, beta={}, trainSize={}): {}",
                                String.format("%.3f", alpha), String.format("%.3f", beta), i, e.getMessage());
                        // 可以考虑给这个参数组合一个非常高的错误值，使其不会被选中
                        // totalAbsoluteError = Double.POSITIVE_INFINITY;
                        // break; // 如果希望某个步骤失败就放弃这个参数组合，可以取消注释 break
                    }
                } // End of CV loop for one (alpha, beta) pair

                // 计算当前参数组合的平均 MAE
                if (cvCount > 0) {
                    double currentMae = totalAbsoluteError / cvCount;
                    log.trace("测试参数: alpha={}, beta={}, CV MAE={}", String.format("%.3f", alpha), String.format("%.3f", beta), String.format("%.4f", currentMae));

                    // 如果当前 MAE 更好，则更新最佳参数
                    // 添加一个小扰动防止完全相等的 MAE 导致不必要的更新 (可选)
                    if (currentMae < minMae - 1e-9) {
                        minMae = currentMae;
                        bestAlpha = alpha;
                        bestBeta = beta;
                        // log.debug("找到新的最优参数: alpha={}, beta={}, CV MAE={}", String.format("%.3f", bestAlpha), String.format("%.3f", bestBeta), String.format("%.4f", minMae));
                    }
                } else {
                    log.warn("参数 alpha={}, beta={} 在 CV 中未能完成任何有效的预测步骤。", String.format("%.3f", alpha), String.format("%.3f", beta));
                }
            } // End of beta loop
        } // End of alpha loop

        // 如果从未成功计算过 MAE（例如所有 CV 步骤都失败），则返回默认值
        if (minMae == Double.POSITIVE_INFINITY) {
            log.error("Holt 参数优化未能找到任何有效的参数组合，将使用默认值 alpha={}, beta={}", DEFAULT_ALPHA, DEFAULT_BETA);
            return new double[]{DEFAULT_ALPHA, DEFAULT_BETA};
        }

        log.debug("参数优化完成。最终选择: alpha={}, beta={}, 最小 CV MAE={}", String.format("%.3f", bestAlpha), String.format("%.3f", bestBeta), String.format("%.4f", minMae));
        return new double[]{bestAlpha, bestBeta};
    }


    /**
     * [内部方法] 使用双指数平滑(Holt)方法预测(无季节性)，使用指定参数。
     * (保持不变)
     */
    private static double[] forecastHoltNonSeasonalInternal(double[] data, int periods, double alpha, double beta) {
        if (data == null || data.length == 0) {
            return new double[periods]; // 或者抛出异常
        }
        if (periods <= 0) {
            return new double[0];
        }
        if (data.length < 2) {
            // 如果只有一个点，无法计算趋势，只能做简单复制或均值预测
            log.warn("Holt 内部预测：数据点不足2个，无法计算趋势。将复制最后一个值。");
            double lastValue = data.length == 1 ? data[0] : 0.0;
            double[] forecast = new double[periods];
            Arrays.fill(forecast, Math.max(0, lastValue));
            return forecast;
        }


        // 初始化 level 和 trend
        double level = data[0];
        // 对初始趋势的计算可以有多种方法，这里保持原来的逻辑
        double trend = calculateInitialTrendInternal(data);

        // 迭代计算 level 和 trend
        for (int i = 1; i < data.length; i++) {
            double prevLevel = level;
            // Holt's equations
            level = alpha * data[i] + (1 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1 - beta) * trend;
        }

        // 生成未来预测
        double[] forecast = new double[periods];
        for (int i = 1; i <= periods; i++) {
            // 预测值 = 最后一个 level + i * 最后一个 trend
            double forecastValue = level + i * trend;
            // 确保预测值非负
            forecast[i - 1] = Math.max(0, forecastValue);
        }
        return forecast;
    }

    /**
     * [内部方法] 计算初始趋势。
     * (保持不变)
     */
    private static double calculateInitialTrendInternal(double[] data) {
        if (data == null || data.length < 2) return 0;
        // 使用前 min(n, 5) 个点的平均差值作为初始趋势估计
        int n = Math.min(data.length, 5); // 最多用前5个点
        if (n < 2) return data.length > 1 ? data[1] - data[0] : 0; // 至少需要2个点

        double sumDiff = 0;
        for (int i = 1; i < n; i++) {
            sumDiff += (data[i] - data[i - 1]);
        }
        // 平均趋势
        return sumDiff / (n - 1);
    }



    /**
     * [内部方法] 计算 MAE (平均绝对误差)。
     * 注意：这个内部方法用于计算 *样本内* 拟合误差，其对齐可能需要仔细检查。
     * 在交叉验证中，我们是显式地计算预测值和实际值的误差。
     */
    private static double calculateMAEInternal(double[] actual, double[] forecast) {
        if (actual == null || forecast == null || actual.length == 0) {
            return Double.NaN;
        }
        // 比较长度应该取两者中较短的，因为预测可能比实际少（如果预测的是未来）
        // 或者预测可能比实际多（如果预测包含了历史拟合值）
        // 在这里，假设是比较训练集上的拟合，长度应一致
        int len = Math.min(actual.length, forecast.length);
        if (len == 0) return Double.NaN;

        double sumAbsError = 0;
        // 注意：Holt 模型的第一个预测值（fitted[1]）通常是基于 data[0] 和初始趋势得到的
        // 而 fitted[0] 可能没有意义或者就是 data[0]。这里简单地从头比较。
        // 更精确的比较可能需要跳过初始几个点。
        int count = 0;
        for (int i = 0; i < len; i++) {
            // 可以选择跳过初始点，例如 i > 0
            sumAbsError += Math.abs(actual[i] - forecast[i]);
            count++;
        }
        return count > 0 ? sumAbsError / count : Double.NaN;
    }


    // --- ForecastStrategy 接口实现 ---

    @Override
    public boolean canHandle(int dataLength) {
        // 策略适用于至少有 MIN_DATA_LENGTH 个数据点的情况
        return dataLength >= MIN_DATA_LENGTH;
    }

    @Override
    public String getStrategyName() {
        return "Holt Non-Seasonal (Optimized)"; // 更新名称
    }

    @Override
    public int getMinDataLength() {
        // 返回该策略适用的最小数据长度，用于排序
        return MIN_DATA_LENGTH;
    }
}