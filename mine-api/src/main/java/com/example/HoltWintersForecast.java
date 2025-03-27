package com.example;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 提供 Holt-Winters (三重指数平滑) 和 Holt (双指数平滑) 算法的核心实现及相关工具方法。
 * <p>
 * 主要职责:
 * 1. 实现季节性和非季节性指数平滑的核心计算逻辑。
 * 2. 提供参数优化方法 (基于交叉验证)。
 * 3. 提供数据评估指标计算方法 (MAPE, RMSE, MAE)。
 * 4. 提供数据平滑工具方法 (如移动平均)。
 * <p>
 * 注意: 此类不再负责模型选择或自动执行整个预测流程，这些职责转移到策略类和 ForecastService。
 */
@Slf4j
public class HoltWintersForecast {

    // #########################################################################
    // # Constants and Enums
    // #########################################################################

    /**
     * 季节性模型类型
     */
    public enum SeasonalityType {
        MULTIPLICATIVE, // 乘法季节性
        ADDITIVE        // 加法季节性
    }

    // 默认季节长度（年度季节性）
    public static final int DEFAULT_DAILY_SEASON_LENGTH = 365;

    // #########################################################################
    // # Core Holt-Winters Seasonal Algorithm (Instance-based for state)
    // #########################################################################

    private double alpha;           // 水平平滑系数
    private double beta;            // 趋势平滑系数
    private double gamma;           // 季节性平滑系数
    private final int seasonLength;  // 季节周期长度
    private final SeasonalityType seasonalityType; // 季节性模型类型

    // 模型状态
    private double level;           // 当前水平
    private double trend;           // 当前趋势
    private double[] seasonal;      // 当前季节性因子
    private boolean initialized = false; // 模型是否已初始化
    private int lastObservationIndex = -1; // 训练数据最后一个点的索引

    // 拟合和残差（可选，用于评估）
    private double[] fittedValues;
    private double[] residuals;


    /**
     * 构造函数 (用于季节性模型实例)
     *
     * @param alpha           水平平滑系数 (0 < alpha < 1)
     * @param beta            趋势平滑系数 (0 <= beta < 1)
     * @param gamma           季节性平滑系数 (0 < gamma < 1)
     * @param seasonLength    季节周期长度 (例如 365 代表年度季节性)
     * @param seasonalityType 季节性模型类型 (乘法或加法)
     */
    public HoltWintersForecast(double alpha, double beta, double gamma, int seasonLength, SeasonalityType seasonalityType) {
        validateSeasonalParameters(alpha, beta, gamma, seasonLength);
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.seasonLength = seasonLength;
        this.seasonalityType = seasonalityType;
        this.seasonal = new double[seasonLength];
    }

    /**
     * 验证季节性模型参数的合法性。
     */
    private static void validateSeasonalParameters(double alpha, double beta, double gamma, int seasonLength) {
        if (alpha <= 0 || alpha >= 1) throw new IllegalArgumentException("Alpha 必须在 (0, 1) 之间");
        if (beta < 0 || beta >= 1) throw new IllegalArgumentException("Beta 必须在 [0, 1) 之间");
        if (gamma <= 0 || gamma >= 1) throw new IllegalArgumentException("Gamma 必须在 (0, 1) 之间");
        if (seasonLength <= 1) throw new IllegalArgumentException("季节长度必须大于 1");
    }

    /**
     * 使用历史数据初始化季节性模型的状态。
     * 至少需要两个完整季节的数据。
     *
     * @param historicalData 历史数据数组。
     * @throws IllegalArgumentException 如果数据不足。
     */
    public void initializeSeasonalModel(double[] historicalData) {
        if (historicalData == null || historicalData.length < 2 * seasonLength) {
            throw new IllegalArgumentException("季节性模型初始化需要至少 " + (2 * seasonLength) + " 个数据点");
        }

        // 1. 初始化水平 (Level) - 使用第一个季节的平均值
        double firstSeasonSum = 0;
        for (int i = 0; i < seasonLength; i++) {
            firstSeasonSum += historicalData[i];
        }
        this.level = firstSeasonSum / seasonLength;

        // 2. 初始化趋势 (Trend) - 使用前两个季节的平均差异
        double firstTwoSeasonsDiffSum = 0;
        for (int i = 0; i < seasonLength; i++) {
            firstTwoSeasonsDiffSum += (historicalData[seasonLength + i] - historicalData[i]);
        }
        // 如果 beta 为 0，则趋势为 0
        this.trend = (beta > 0) ? firstTwoSeasonsDiffSum / (seasonLength * seasonLength) : 0;

        // 3. 初始化季节性因子 (Seasonal Factors)
        this.seasonal = new double[seasonLength];
        for (int i = 0; i < seasonLength; i++) {
            if (seasonalityType == SeasonalityType.MULTIPLICATIVE) {
                // 防止除以零
                this.seasonal[i] = historicalData[i] / (this.level == 0 ? 1e-6 : this.level);
            } else { // ADDITIVE
                this.seasonal[i] = historicalData[i] - this.level;
            }
        }

        // 4. 归一化季节性因子
        normalizeSeasonalFactors();

        this.initialized = true;
        this.lastObservationIndex = -1; // 重置训练索引
        log.debug("季节性模型初始化完成: Level={}, Trend={}", this.level, this.trend);
    }


    /**
     * 归一化季节性因子，确保乘法模型因子之和约为 seasonLength，加法模型因子之和约为 0。
     */
    private void normalizeSeasonalFactors() {
        if (seasonal == null || seasonal.length == 0) return;

        double factorSum = Arrays.stream(seasonal).sum();
        double adjustment;

        if (seasonalityType == SeasonalityType.MULTIPLICATIVE) {
            // 乘法模型：因子平均值应为 1
            adjustment = seasonLength / (factorSum == 0 ? 1e-6 : factorSum);
            for (int i = 0; i < seasonLength; i++) {
                seasonal[i] *= adjustment;
            }
        } else { // ADDITIVE
            // 加法模型：因子总和应为 0
            adjustment = factorSum / seasonLength;
            for (int i = 0; i < seasonLength; i++) {
                seasonal[i] -= adjustment;
            }
        }
    }


    /**
     * 使用给定的训练数据更新季节性模型的状态。
     * 必须先调用 initializeSeasonalModel。
     *
     * @param trainingData 训练数据数组。
     * @throws IllegalStateException 如果模型未初始化。
     */
    public void trainSeasonalModel(double[] trainingData) {
        if (!initialized) {
            throw new IllegalStateException("季节性模型必须先初始化");
        }
        if (trainingData == null || trainingData.length == 0) {
            log.warn("训练数据为空，跳过训练");
            return;
        }

        // 初始化拟合值和残差数组（如果需要记录）
        this.fittedValues = new double[trainingData.length];
        this.residuals = new double[trainingData.length];

        double prevLevel, prevTrend;
        double currentObservation;

        for (int i = 0; i < trainingData.length; i++) {
            currentObservation = trainingData[i];
            // 注意：这里的季节索引应该基于整个时间序列的位置，而不是训练数据的索引
            // 如果训练数据不是从时间序列的开始，需要调整 seasonIndex 的计算
            // 假设 trainingData 是紧接着初始化数据之后的数据，或者从头开始
            int seasonIndex = i % seasonLength; // 简化假设：训练数据从季节开始

            prevLevel = this.level;
            prevTrend = this.trend;

            if (seasonalityType == SeasonalityType.MULTIPLICATIVE) {
                // 乘法模型更新
                double seasonalFactor = this.seasonal[seasonIndex];
                // 防止除以零
                double levelDenominator = (prevLevel + prevTrend);
                levelDenominator = (levelDenominator == 0) ? 1e-6 : levelDenominator;
                double observationOverSeasonal = currentObservation / (seasonalFactor == 0 ? 1e-6 : seasonalFactor);

                this.level = alpha * observationOverSeasonal + (1 - alpha) * (prevLevel + prevTrend);
                this.trend = beta * (this.level - prevLevel) + (1 - beta) * prevTrend;
                this.seasonal[seasonIndex] = gamma * (currentObservation / (this.level == 0 ? 1e-6 : this.level)) + (1 - gamma) * seasonalFactor;

                // 计算拟合值
                this.fittedValues[i] = (prevLevel + prevTrend) * seasonalFactor;

            } else { // ADDITIVE
                // 加法模型更新
                double seasonalComponent = this.seasonal[seasonIndex];
                double observationMinusSeasonal = currentObservation - seasonalComponent;

                this.level = alpha * observationMinusSeasonal + (1 - alpha) * (prevLevel + prevTrend);
                this.trend = beta * (this.level - prevLevel) + (1 - beta) * prevTrend;
                this.seasonal[seasonIndex] = gamma * (currentObservation - this.level) + (1 - gamma) * seasonalComponent;

                // 计算拟合值
                this.fittedValues[i] = prevLevel + prevTrend + seasonalComponent;
            }

            // 计算残差
            this.residuals[i] = currentObservation - this.fittedValues[i];

            // 每次更新季节性因子后都进行归一化，保持稳定性
            normalizeSeasonalFactors();
        }
        this.lastObservationIndex = trainingData.length - 1;
        log.debug("季节性模型训练完成。最终 Level={}, Trend={}", this.level, this.trend);
    }

    /**
     * 使用训练好的季节性模型进行预测。
     *
     * @param periods 要预测的未来期数。
     * @return 包含未来每日预测值的数组。
     * @throws IllegalStateException 如果模型未训练。
     */
    public double[] forecastSeasonal(int periods) {
        if (!initialized || lastObservationIndex < 0) { // 确保初始化和训练过
            throw new IllegalStateException("季节性模型尚未初始化或训练");
        }
        if (periods <= 0) {
            return new double[0];
        }

        double[] forecasts = new double[periods];
        for (int i = 1; i <= periods; i++) {
            // 计算预测点对应的季节索引，相对于训练数据最后一个点
            int seasonIndex = (this.lastObservationIndex + i) % seasonLength;
            double forecastValue;

            if (seasonalityType == SeasonalityType.MULTIPLICATIVE) {
                forecastValue = (this.level + i * this.trend) * this.seasonal[seasonIndex];
            } else { // ADDITIVE
                forecastValue = this.level + i * this.trend + this.seasonal[seasonIndex];
            }

            // 确保预测值非负
            forecasts[i - 1] = Math.max(0, forecastValue);
        }
        return forecasts;
    }


    // #########################################################################
    // # Core Holt Non-Seasonal Algorithm (Static Methods)
    // #########################################################################

     /**
     * 使用双指数平滑(Holt)方法预测(无季节性)，使用指定参数。
     * 这是一个静态方法，因为它不维护季节性状态。
     *
     * @param data    历史数据数组。
     * @param periods 预测期数。
     * @param alpha   水平平滑系数 (0 < alpha < 1)。
     * @param beta    趋势平滑系数 (0 <= beta < 1)。
     * @return 包含未来每日预测值的数组。
     * @throws IllegalArgumentException 如果数据不足或参数无效。
     */
    public static double[] forecastHoltNonSeasonal(double[] data, int periods, double alpha, double beta) {
        if (data == null || data.length < 2) {
            throw new IllegalArgumentException("Holt 非季节性预测至少需要 2 个数据点");
        }
        validateNonSeasonalParameters(alpha, beta);
        if (periods <= 0) {
            return new double[0];
        }

        // 初始化水平和趋势
        double level = data[0];
        // 稍微稳健的趋势初始化
        double trend = calculateInitialTrend(data);

        // 根据历史数据更新水平和趋势
        for (int i = 1; i < data.length; i++) {
            double prevLevel = level;
            // Holt 更新公式
            level = alpha * data[i] + (1 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1 - beta) * trend;
        }

        // 预测
        double[] forecast = new double[periods];
        for (int i = 1; i <= periods; i++) {
            double forecastValue = level + i * trend;
            forecast[i - 1] = Math.max(0, forecastValue); // 确保预测值非负
        }

        return forecast;
    }

    /**
     * 验证非季节性模型参数 (alpha, beta)。
     */
    private static void validateNonSeasonalParameters(double alpha, double beta) {
        if (alpha <= 0 || alpha >= 1) throw new IllegalArgumentException("Alpha 必须在 (0, 1) 之间");
        if (beta < 0 || beta >= 1) throw new IllegalArgumentException("Beta 必须在 [0, 1) 之间");
    }

    /**
     * 计算初始趋势的更稳健方法 (例如，前几个点的平均差异)。
     */
    private static double calculateInitialTrend(double[] data) {
        if (data.length < 2) return 0;
        int n = Math.min(data.length, 5); // 使用最多前5个点
        if (n < 2) return data.length > 1 ? data[1] - data[0] : 0;

        double sumDiff = 0;
        for (int i = 1; i < n; i++) {
            sumDiff += (data[i] - data[i-1]);
        }
        return sumDiff / (n - 1);
    }


    // #########################################################################
    // # Parameter Optimization (Static Methods)
    // #########################################################################

    /**
     * 使用网格搜索和时间序列交叉验证为季节性模型找到最优参数 (alpha, beta, gamma)。
     *
     * @param trainingData    训练数据
     * @param alphaValues     Alpha 参数候选值数组。
     * @param betaValues      Beta 参数候选值数组。
     * @param gammaValues     Gamma 参数候选值数组。
     * @param seasonLength    季节长度。
     * @param seasonalityType 季节性类型。
     * @param useMAE          是否使用 MAE (均值绝对误差) 代替 MAPE (均值绝对百分比误差) 进行评估，MAE 对 0 值更鲁棒。
     * @return 包含最优 [alpha, beta, gamma] 的数组。
     * @throws IllegalArgumentException 如果数据不足。
     */
    public static double[] findOptimalSeasonalParametersCV(double[] trainingData,
                                                           double[] alphaValues,
                                                           double[] betaValues,
                                                           double[] gammaValues,
                                                           int seasonLength,
                                                           SeasonalityType seasonalityType,
                                                           boolean useMAE) {
        if (trainingData == null || trainingData.length < 3 * seasonLength) {
            throw new IllegalArgumentException("季节性参数优化需要至少 " + (3 * seasonLength) + " 个数据点");
        }

        // 交叉验证设置
        int minTrainSize = 2 * seasonLength; // 至少需要两个季节来初始化
        int validationWindowSize = seasonLength; // 验证窗口为一个季节长度
        int step = Math.max(1, seasonLength / 4); // 滑动步长
        int numFolds = Math.max(3, (trainingData.length - minTrainSize - validationWindowSize) / step + 1);
        numFolds = Math.min(numFolds, 8); // 限制最大折数

        log.info("开始季节性参数优化 ({} 折交叉验证, 评估指标: {})...", numFolds, useMAE ? "MAE" : "MAPE");

        double bestAvgError = Double.MAX_VALUE;
        double[] bestParams = new double[3]; // [alpha, beta, gamma]

        // 默认参数范围 (如果未提供)
        alphaValues = (alphaValues == null || alphaValues.length == 0) ? new double[]{0.05, 0.1, 0.2, 0.3} : alphaValues;
        betaValues = (betaValues == null || betaValues.length == 0) ? new double[]{0.01, 0.05, 0.1, 0.2} : betaValues;
        gammaValues = (gammaValues == null || gammaValues.length == 0) ? new double[]{0.05, 0.1, 0.2, 0.3} : gammaValues;


        for (double alpha : alphaValues) {
            for (double beta : betaValues) {
                for (double gamma : gammaValues) {
                    try {
                        validateSeasonalParameters(alpha, beta, gamma, seasonLength); // 提前验证

                        double totalError = 0;
                        int validFolds = 0;

                        for (int fold = 0; fold < numFolds; fold++) {
                            int validationStart = minTrainSize + fold * step;
                            int validationEnd = Math.min(validationStart + validationWindowSize, trainingData.length);

                            if (validationEnd - validationStart < Math.max(1, seasonLength / 4)) continue; // 验证集太小

                            double[] foldTrainSet = Arrays.copyOfRange(trainingData, 0, validationStart);
                            double[] foldValidationSet = Arrays.copyOfRange(trainingData, validationStart, validationEnd);

                            // 训练模型
                            HoltWintersForecast model = new HoltWintersForecast(alpha, beta, gamma, seasonLength, seasonalityType);
                            // 注意：每次交叉验证都需要重新初始化和训练
                            model.initializeSeasonalModel(foldTrainSet); // 使用部分数据初始化
                            model.trainSeasonalModel(foldTrainSet);      // 使用相同数据训练

                            // 预测
                            double[] forecast = model.forecastSeasonal(foldValidationSet.length);

                            // 计算误差
                            double error = useMAE ? calculateMAE(foldValidationSet, forecast) : calculateMAPE(foldValidationSet, forecast);

                            if (!Double.isNaN(error) && Double.isFinite(error)) {
                                totalError += error;
                                validFolds++;
                            } else {
                                // log.trace("Fold {} for params ({}, {}, {}) resulted in invalid error: {}", fold, alpha, beta, gamma, error);
                            }
                        }

                        if (validFolds >= numFolds / 2) { // 至少一半折数有效
                            double avgError = totalError / validFolds;
                            if (avgError < bestAvgError) {
                                bestAvgError = avgError;
                                bestParams[0] = alpha;
                                bestParams[1] = beta;
                                bestParams[2] = gamma;
                                // log.debug("New best seasonal params found: alpha={}, beta={}, gamma={}, AvgError={}", alpha, beta, gamma, avgError);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // log.trace("跳过无效参数组合: alpha={}, beta={}, gamma={}", alpha, beta, gamma);
                    } catch (Exception e) {
                        log.warn("季节性参数优化中出现意外错误 (alpha={}, beta={}, gamma={}): {}", alpha, beta, gamma, e.getMessage());
                    }
                }
            }
        }

        if (bestAvgError == Double.MAX_VALUE) {
            log.warn("未能找到有效的季节性参数组合，将使用默认值或引发错误。");
            // 可以选择返回默认值或抛出异常
            // return new double[]{0.1, 0.05, 0.1}; // 示例默认值
             throw new RuntimeException("未能找到有效的季节性参数组合");
        }

        log.info("季节性参数优化完成。最优参数: alpha={}, beta={}, gamma={}, 平均误差({}): {}",
                bestParams[0], bestParams[1], bestParams[2], useMAE ? "MAE" : "MAPE", bestAvgError);

        return bestParams;
    }


    /**
     * 使用时间序列交叉验证为 Holt 非季节性模型找到最优参数 (alpha, beta)。
     *
     * @param trainingData 训练数据。
     * @param alphaValues  Alpha 参数候选值数组。
     * @param betaValues   Beta 参数候选值数组。
     * @param useMAE       是否使用 MAE 代替 MAPE 进行评估。
     * @return 包含最优 [alpha, beta] 的数组。
     * @throws IllegalArgumentException 如果数据不足。
     */
    public static double[] findOptimalHoltParametersCV(double[] trainingData,
                                                       double[] alphaValues,
                                                       double[] betaValues,
                                                       boolean useMAE) {
        if (trainingData == null || trainingData.length < 15) { // Holt 需要的数据量相对较少
            throw new IllegalArgumentException("Holt 参数优化需要至少 15 个数据点");
        }

        // 交叉验证设置
        int minTrainSize = Math.max(10, trainingData.length / 3); // 最小训练集
        int validationWindowSize = Math.max(5, trainingData.length / 5); // 验证窗口
        int step = Math.max(1, validationWindowSize / 2); // 滑动步长
        int numFolds = Math.max(3, (trainingData.length - minTrainSize - validationWindowSize) / step + 1);
        numFolds = Math.min(numFolds, 10); // 限制最大折数

        log.info("开始 Holt 非季节性参数优化 ({} 折交叉验证, 评估指标: {})...", numFolds, useMAE ? "MAE" : "MAPE");

        double bestAvgError = Double.MAX_VALUE;
        double[] bestParams = new double[2]; // [alpha, beta]

        // 默认参数范围
        alphaValues = (alphaValues == null || alphaValues.length == 0) ? IntStream.rangeClosed(1, 10).mapToDouble(i -> i * 0.1).toArray() : alphaValues;
        betaValues = (betaValues == null || betaValues.length == 0) ? IntStream.rangeClosed(0, 10).mapToDouble(i -> i * 0.1).toArray() : betaValues;


        for (double alpha : alphaValues) {
            for (double beta : betaValues) {
                try {
                    validateNonSeasonalParameters(alpha, beta); // 提前验证

                    double totalError = 0;
                    int validFolds = 0;

                    for (int fold = 0; fold < numFolds; fold++) {
                        int validationStart = minTrainSize + fold * step;
                        int validationEnd = Math.min(validationStart + validationWindowSize, trainingData.length);

                        if (validationEnd - validationStart < 3) continue; // 验证集至少需要3个点

                        double[] foldTrainSet = Arrays.copyOfRange(trainingData, 0, validationStart);
                        double[] foldValidationSet = Arrays.copyOfRange(trainingData, validationStart, validationEnd);

                        // 使用当前参数进行预测
                        double[] forecast = forecastHoltNonSeasonal(foldTrainSet, foldValidationSet.length, alpha, beta);

                        // 计算误差
                        double error = useMAE ? calculateMAE(foldValidationSet, forecast) : calculateMAPE(foldValidationSet, forecast);

                        if (!Double.isNaN(error) && Double.isFinite(error)) {
                            totalError += error;
                            validFolds++;
                        } else {
                           // log.trace("Fold {} for Holt params ({}, {}) resulted in invalid error: {}", fold, alpha, beta, error);
                        }
                    }

                    if (validFolds >= numFolds / 2) {
                        double avgError = totalError / validFolds;
                        if (avgError < bestAvgError) {
                            bestAvgError = avgError;
                            bestParams[0] = alpha;
                            bestParams[1] = beta;
                            // log.debug("New best Holt params found: alpha={}, beta={}, AvgError={}", alpha, beta, avgError);
                        }
                    }
                } catch (IllegalArgumentException e) {
                   // log.trace("跳过无效 Holt 参数组合: alpha={}, beta={}", alpha, beta);
                } catch (Exception e) {
                    log.warn("Holt 参数优化中出现意外错误 (alpha={}, beta={}): {}", alpha, beta, e.getMessage());
                }
            }
        }

         if (bestAvgError == Double.MAX_VALUE) {
            log.warn("未能找到有效的 Holt 参数组合，将使用默认值或引发错误。");
            // return new double[]{0.2, 0.1}; // 示例默认值
             throw new RuntimeException("未能找到有效的 Holt 参数组合");
        }

        log.info("Holt 非季节性参数优化完成。最优参数: alpha={}, beta={}, 平均误差({}): {}",
                bestParams[0], bestParams[1], useMAE ? "MAE" : "MAPE", bestAvgError);

        return bestParams;
    }


    // #########################################################################
    // # Evaluation Metrics (Static Methods)
    // #########################################################################

    /**
     * 计算平均绝对百分比误差 (MAPE)。
     * 注意：对 0 或接近 0 的实际值非常敏感。
     *
     * @param actual   实际值数组。
     * @param forecast 预测值数组。
     * @return MAPE 值 (0-100)，如果无法计算则返回 NaN。
     * @throws IllegalArgumentException 如果数组长度不匹配。
     */
    public static double calculateMAPE(double[] actual, double[] forecast) {
        if (actual == null || forecast == null || actual.length != forecast.length) {
            throw new IllegalArgumentException("实际值和预测值数组必须非空且长度相同");
        }
        if (actual.length == 0) return Double.NaN; // 或 0

        double sumPercentageError = 0;
        int count = 0;
        for (int i = 0; i < actual.length; i++) {
            if (Math.abs(actual[i]) > 1e-6) { // 避免除以非常接近零的数
                sumPercentageError += Math.abs((actual[i] - forecast[i]) / actual[i]);
                count++;
            }
        }
        // 如果所有实际值都接近于0，MAPE可能无意义
        return (count > 0) ? (sumPercentageError / count) * 100.0 : Double.NaN;
    }

    /**
     * 计算均方根误差 (RMSE)。
     *
     * @param actual   实际值数组。
     * @param forecast 预测值数组。
     * @return RMSE 值。
     * @throws IllegalArgumentException 如果数组长度不匹配。
     */
    public static double calculateRMSE(double[] actual, double[] forecast) {
        if (actual == null || forecast == null || actual.length != forecast.length) {
            throw new IllegalArgumentException("实际值和预测值数组必须非空且长度相同");
        }
        if (actual.length == 0) return Double.NaN; // 或 0

        double sumSquaredError = 0;
        for (int i = 0; i < actual.length; i++) {
            double error = actual[i] - forecast[i];
            sumSquaredError += error * error;
        }
        return Math.sqrt(sumSquaredError / actual.length);
    }

    /**
     * 计算平均绝对误差 (MAE)。对异常值和零值比 RMSE 和 MAPE 更不敏感。
     *
     * @param actual   实际值数组。
     * @param forecast 预测值数组。
     * @return MAE 值。
     * @throws IllegalArgumentException 如果数组长度不匹配。
     */
    public static double calculateMAE(double[] actual, double[] forecast) {
        if (actual == null || forecast == null || actual.length != forecast.length) {
            throw new IllegalArgumentException("实际值和预测值数组必须非空且长度相同");
        }
        if (actual.length == 0) return Double.NaN; // 或 0

        double sumAbsError = 0;
        for (int i = 0; i < actual.length; i++) {
            sumAbsError += Math.abs(actual[i] - forecast[i]);
        }
        return sumAbsError / actual.length;
    }


    // #########################################################################
    // # Utility Methods (Static)
    // #########################################################################

     /**
     * 对原始数据应用简单的移动平均平滑处理。
     *
     * @param rawData    原始数据数组。
     * @param windowSize 移动平均窗口大小。
     * @return 平滑处理后的数据数组。
     */
    public static double[] applySimpleMovingAverage(double[] rawData, int windowSize) {
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
     * 获取模型评估指标 (如果模型实例被训练过)。
     *
     * @return 包含各项评估指标的数组 [MAPE, RMSE, MAE]，如果未训练则抛出异常。
     * @throws IllegalStateException 如果模型实例未被训练。
     */
    public double[] getSeasonalModelMetrics() {
        if (fittedValues == null || residuals == null || lastObservationIndex < 0) {
            throw new IllegalStateException("季节性模型尚未训练或无拟合/残差数据");
        }

        // 从拟合值和残差中恢复实际值
        double[] actualValues = new double[fittedValues.length];
        for (int i = 0; i < fittedValues.length; i++) {
            actualValues[i] = fittedValues[i] + residuals[i];
        }

        double mape = calculateMAPE(actualValues, fittedValues);
        double rmse = calculateRMSE(actualValues, fittedValues);
        double mae = calculateMAE(actualValues, fittedValues);

        return new double[]{mape, rmse, mae};
    }

    /**
     * 获取当前季节性模型实例的参数。
     *
     * @return 模型参数的字符串表示。
     */
    public String getSeasonalModelParameters() {
         if (!initialized) {
            return "季节性模型未初始化";
        }
        return String.format("模型类型: %s, 季节长度: %d\nAlpha: %.4f, Beta: %.4f, Gamma: %.4f\n" +
                        "当前 Level: %.4f, 当前 Trend: %.4f\n当前季节性因子: %s",
                seasonalityType, seasonLength, alpha, beta, gamma,
                level, trend, Arrays.toString(seasonal));
    }

    // --- 移除或注释掉不再需要的旧方法 ---
    /*
    public static double[] findOptimalParameters(...) // 旧的优化方法
    public static double[] findOptimalParametersWithCV(...) // 旧的优化方法
    public double[] forecast(int periods) // 旧的实例预测方法
    public void train(double[] trainingData) // 旧的实例训练方法
    public boolean initialize(double[] historicalData) // 旧的实例初始化方法
    public static HoltWintersForecast createOptimizedModel(...) // 旧的便捷创建方法
    public static double[] dailyForecast(...) // 旧的统一入口方法
    public static boolean shouldUseSeasonalModel(...) // 职责转移到策略类
    public static double[] findOptimalHoltParameters(...) // 旧的Holt优化
    public static double[] findOptimalHoltParametersWithCV(...) // 旧的Holt优化CV
    public static double[] forecastWithHoltMethod(...) // 旧的Holt预测入口
    */

    /**
     * 使用示例 (可保留用于单独测试核心算法)
     */
    public static void main(String[] args) {
        // 示例：测试季节性模型
        System.out.println("=============== 季节性模型测试 ===============");
        double[] seasonalData = { /* ... 提供至少两个季节的数据 ... */ 8, 12, 15, 10, 8, 12, 15, 10, 9, 13, 16, 11 }; // 示例月度数据, season=4
        int seasonLength = 4;
        try {
            // 1. 优化参数
            double[] optimalParams = findOptimalSeasonalParametersCV(seasonalData, null, null, null, seasonLength, SeasonalityType.ADDITIVE, true);
            // 2. 创建模型实例
            HoltWintersForecast seasonalModel = new HoltWintersForecast(optimalParams[0], optimalParams[1], optimalParams[2], seasonLength, SeasonalityType.ADDITIVE);
            // 3. 初始化和训练
            seasonalModel.initializeSeasonalModel(seasonalData);
            seasonalModel.trainSeasonalModel(seasonalData); // 再次训练以更新状态
            // 4. 预测
            int forecastPeriods = 8;
            double[] seasonalForecast = seasonalModel.forecastSeasonal(forecastPeriods);
            System.out.println("季节性预测结果: " + Arrays.toString(seasonalForecast));
            System.out.println("模型参数:\n" + seasonalModel.getSeasonalModelParameters());

        } catch (Exception e) {
            System.err.println("季节性模型测试失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 示例：测试非季节性模型 (Holt)
        System.out.println("\n=============== 非季节性模型 (Holt) 测试 ===============");
        double[] nonSeasonalData = { 10, 12, 13, 16, 19, 23, 26, 30, 29, 32, 31, 35, 38, 40 };
        try {
            // 1. 优化参数
            double[] optimalHoltParams = findOptimalHoltParametersCV(nonSeasonalData, null, null, true);
            // 2. 预测
            int holtForecastPeriods = 5;
            double[] holtForecast = forecastHoltNonSeasonal(nonSeasonalData, holtForecastPeriods, optimalHoltParams[0], optimalHoltParams[1]);
            System.out.println("Holt 非季节性预测结果: " + Arrays.toString(holtForecast));
        } catch (Exception e) {
            System.err.println("Holt 非季节性模型测试失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 示例：测试移动平均
        System.out.println("\n=============== 移动平均测试 ===============");
        double[] maData = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20};
        int window = 3;
        double[] smoothed = applySimpleMovingAverage(maData, window);
        System.out.println("原始数据: " + Arrays.toString(maData));
        System.out.println(window + "期移动平均: " + Arrays.toString(smoothed));
    }
}
