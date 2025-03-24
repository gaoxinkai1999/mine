package com.example;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 三重指数平滑(Holt-Winters)算法实现
 * 适用于具有季节性的时间序列数据预测
 * <p>
 * 更新：
 * 1. 优化日度数据参数范围
 * 2. 为双指数平滑降级方法增加参数优化功能
 * 3. 专注于年季节性预测，针对日度数据特点优化
 * 4. 增强零值和高波动数据处理能力
 */
@Slf4j
public class HoltWintersForecast {

    /**
     * 季节性模型类型
     */
    public enum SeasonalityType {
        MULTIPLICATIVE,  // 乘法季节性
        ADDITIVE         // 加法季节性
    }

    // 算法参数
    private double alpha;          // 水平平滑系数
    private double beta;           // 趋势平滑系数
    private double gamma;          // 季节性平滑系数
    private final int seasonLength; // 季节周期长度
    private final SeasonalityType seasonalityType; // 季节性模型类型

    // 预测所需的模型组件
    private double level;          // 序列水平
    private double trend;          // 序列趋势
    private double[] seasonal;     // 季节性因子

    // 历史拟合结果与误差
    private double[] fittedValues;
    private double[] residuals;

    // 训练集中的最后一个观测值的索引
    private int lastObservationIndex;

    /**
     * 构造函数
     *
     * @param alpha           水平平滑系数 (0 < alpha < 1)
     * @param beta            趋势平滑系数 (0 < beta < 1)
     * @param gamma           季节性平滑系数 (0 < gamma < 1)
     * @param seasonLength    季节周期长度(如按月数据为12，按周数据为52，日度数据使用365)
     * @param seasonalityType 季节性模型类型(乘法或加法)
     */
    public HoltWintersForecast(double alpha, double beta, double gamma, int seasonLength,
                               SeasonalityType seasonalityType) {
        validateParameters(alpha, beta, gamma, seasonLength);

        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.seasonLength = seasonLength;
        this.seasonalityType = seasonalityType;
        this.seasonal = new double[seasonLength];
    }

    /**
     * 构造函数 - 默认使用乘法季节性模型
     */
    public HoltWintersForecast(double alpha, double beta, double gamma, int seasonLength) {
        this(alpha, beta, gamma, seasonLength, SeasonalityType.MULTIPLICATIVE);
    }

    /**
     * 验证参数合法性
     */
    private void validateParameters(double alpha, double beta, double gamma, int seasonLength) {
        if (alpha <= 0 || alpha >= 1) {
            throw new IllegalArgumentException("Alpha必须在0和1之间(不含0和1)");
        }
        if (beta < 0 || beta >= 1) {
            throw new IllegalArgumentException("Beta必须在0和1之间(包含0，不含1)，0表示无趋势");
        }
        if (gamma <= 0 || gamma >= 1) {
            throw new IllegalArgumentException("Gamma必须在0和1之间(不含0和1)");
        }
        if (seasonLength <= 1) {
            throw new IllegalArgumentException("季节长度必须至少为2");
        }
    }

    /**
     * 使用历史数据初始化模型
     *
     * @param historicalData 历史销量数据
     * @return 是否成功初始化
     */
    public boolean initialize(double[] historicalData) {
        if (historicalData == null || historicalData.length < 2 * seasonLength) {
            throw new IllegalArgumentException(
                    "历史数据必须至少包含两个完整的季节周期(最少需要" + (2 * seasonLength) + "个观测值)");
        }

        // 初始化结果数组
        fittedValues = new double[historicalData.length];
        residuals = new double[historicalData.length];
        lastObservationIndex = historicalData.length - 1;

        // 初始化水平值(取第一个季节的平均值)
        double sum = 0;
        for (int i = 0; i < seasonLength; i++) {
            sum += historicalData[i];
        }
        level = sum / seasonLength;

        // 初始化趋势值
        if (beta > 0) {
            trend = 0;
            for (int i = 0; i < seasonLength; i++) {
                trend += (historicalData[seasonLength + i] - historicalData[i]);
            }
            trend = trend / (seasonLength * seasonLength);
        } else {
            trend = 0;  // 如果beta为0，则没有趋势
        }

        // 初始化季节性因子
        for (int i = 0; i < seasonLength; i++) {
            if (seasonalityType == SeasonalityType.MULTIPLICATIVE) {
                seasonal[i] = historicalData[i] / (level == 0 ? 0.0001 : level);
            } else {
                seasonal[i] = historicalData[i] - level;
            }
        }

        // 归一化季节性因子
        normalizeSeasonalFactors();

        return true;
    }

    /**
     * 归一化季节性因子，使其均值为1(乘法模型)或0(加法模型)
     */
    private void normalizeSeasonalFactors() {
        double sum = 0;
        for (double factor : seasonal) {
            sum += factor;
        }

        if (seasonalityType == SeasonalityType.MULTIPLICATIVE) {
            // 乘法模型中因子均值应为1
            double meanFactor = sum / seasonLength;
            if (meanFactor != 0) {
                for (int i = 0; i < seasonLength; i++) {
                    seasonal[i] /= meanFactor;
                }
            }
        } else {
            // 加法模型中因子均值应为0
            double meanFactor = sum / seasonLength;
            for (int i = 0; i < seasonLength; i++) {
                seasonal[i] -= meanFactor;
            }
        }
    }

    /**
     * 对模型进行训练
     *
     * @param trainingData 训练数据
     */
    public void train(double[] trainingData) {
        if (trainingData == null || trainingData.length <= seasonLength) {
            throw new IllegalArgumentException("训练数据不足");
        }

        // 确保模型已初始化
        if (seasonal[0] == 0) {
            if (!initialize(trainingData)) {
                throw new IllegalStateException("模型初始化失败");
            }
        }

        double[] y = trainingData;
        double prevLevel, prevTrend;

        // 计算第一个季节周期的拟合值
        for (int i = 0; i < seasonLength; i++) {
            if (seasonalityType == SeasonalityType.MULTIPLICATIVE) {
                fittedValues[i] = (level + trend * (i + 1)) * seasonal[i];
            } else {
                fittedValues[i] = level + trend * (i + 1) + seasonal[i];
            }
            residuals[i] = y[i] - fittedValues[i];
        }

        // 使用训练数据更新模型参数
        for (int i = seasonLength; i < y.length; i++) {
            int season = i % seasonLength;

            prevLevel = level;
            prevTrend = trend;

            if (seasonalityType == SeasonalityType.MULTIPLICATIVE) {
                // 乘法模型更新公式
                double denominator = seasonal[season] == 0 ? 0.0001 : seasonal[season]; // 防止除零
                level = alpha * (y[i] / denominator) + (1 - alpha) * (prevLevel + prevTrend);
                trend = beta * (level - prevLevel) + (1 - beta) * prevTrend;

                denominator = level == 0 ? 0.0001 : level; // 防止除零
                seasonal[season] = gamma * (y[i] / denominator) + (1 - gamma) * seasonal[season];

                // 计算拟合值
                fittedValues[i] = (prevLevel + prevTrend) * seasonal[season];
            } else {
                // 加法模型更新公式
                level = alpha * (y[i] - seasonal[season]) + (1 - alpha) * (prevLevel + prevTrend);
                trend = beta * (level - prevLevel) + (1 - beta) * prevTrend;
                seasonal[season] = gamma * (y[i] - level) + (1 - gamma) * seasonal[season];

                // 计算拟合值
                fittedValues[i] = prevLevel + prevTrend + seasonal[season];
            }

            // 计算残差
            residuals[i] = y[i] - fittedValues[i];
        }

        // 季节性因子再归一化，确保平均值符合要求
        normalizeSeasonalFactors();
    }

    /**
     * 使用网格搜索找到最优参数（针对日度数据优化参数范围）
     *
     * @param trainingData    训练数据
     * @param alphaValues     待测试的alpha值数组
     * @param betaValues      待测试的beta值数组
     * @param gammaValues     待测试的gamma值数组
     * @param seasonLength    季节长度
     * @param seasonalityType 季节性类型
     * @return 最优参数组合
     */
    public static double[] findOptimalParameters(double[] trainingData, double[] alphaValues,
                                                 double[] betaValues, double[] gammaValues,
                                                 int seasonLength, SeasonalityType seasonalityType) {
        if (trainingData.length < 3 * seasonLength) {
            throw new IllegalArgumentException("参数优化需要至少3个季节的数据");
        }

        // 将数据分为训练集和验证集
        int validationStart = trainingData.length - seasonLength;
        double[] trainSet = Arrays.copyOfRange(trainingData, 0, validationStart);
        double[] validationSet = Arrays.copyOfRange(trainingData, validationStart, trainingData.length);

        double bestMAPE = Double.MAX_VALUE;
        double[] bestParams = new double[3];

        // 网格搜索
        for (double alpha : alphaValues) {
            for (double beta : betaValues) {
                for (double gamma : gammaValues) {
                    try {
                        HoltWintersForecast model = new HoltWintersForecast(alpha, beta, gamma,
                                seasonLength, seasonalityType);
                        model.train(trainSet);

                        // 预测验证集
                        double[] forecast = model.forecast(validationSet.length);

                        // 计算MAPE
                        double mape = calculateMAPE(validationSet, forecast);

                        // 更新最优参数
                        if (mape < bestMAPE) {
                            bestMAPE = mape;
                            bestParams[0] = alpha;
                            bestParams[1] = beta;
                            bestParams[2] = gamma;
                        }
                    } catch (Exception e) {
                        // 忽略无效参数组合
                        System.err.println("无效参数组合: " +
                                alpha + ", " + beta + ", " + gamma + " - " + e.getMessage());
                    }
                }
            }
        }

        System.out.println("最优参数: alpha=" + bestParams[0] +
                ", beta=" + bestParams[1] + ", gamma=" + bestParams[2] +
                ", MAPE=" + bestMAPE);

        return bestParams;
    }

    /**
     * 使用时间序列交叉验证找到最优参数
     * @param trainingData 训练数据
     * @param alphaValues alpha参数候选值
     * @param betaValues beta参数候选值
     * @param gammaValues gamma参数候选值
     * @param seasonLength 季节长度
     * @param seasonalityType 季节性类型
     * @return 最优参数组合
     */
    public static double[] findOptimalParametersWithCV(double[] trainingData, 
                                                 double[] alphaValues,
                                                 double[] betaValues, 
                                                 double[] gammaValues,
                                                 int seasonLength, 
                                                 SeasonalityType seasonalityType) {
        if (trainingData.length < 3 * seasonLength) {
            throw new IllegalArgumentException("参数优化需要至少3个季节的数据");
        }

        // 设置交叉验证参数
        int minTrainSize = 2 * seasonLength; // 最小训练集大小
        int windowSize = seasonLength; // 验证窗口大小
        int step = Math.max(1, seasonLength / 4); // 滑动步长(季节长度的四分之一)
        int numFolds = Math.max(3, (trainingData.length - minTrainSize - windowSize) / step + 1); // 至少3个折

        // 限制折数，避免过多计算
        numFolds = Math.min(numFolds, 8);
        
        System.out.println("执行" + numFolds + "折时间序列交叉验证...");

        double bestAverageMAPE = Double.MAX_VALUE;
        double[] bestParams = new double[3];

        // 网格搜索
        for (double alpha : alphaValues) {
            for (double beta : betaValues) {
                for (double gamma : gammaValues) {
                    try {
                        double totalMAPE = 0;
                        int validFolds = 0;

                        // 对每个交叉验证折进行评估
                        for (int fold = 0; fold < numFolds; fold++) {
                            int validationStart = minTrainSize + fold * step;
                            int validationEnd = Math.min(validationStart + windowSize, trainingData.length);
                            
                            // 确保验证集大小合理
                            if (validationEnd - validationStart < seasonLength / 2) {
                                continue;
                            }

                            // 提取当前折的训练集和验证集
                            double[] foldTrainSet = Arrays.copyOfRange(trainingData, 0, validationStart);
                            double[] foldValidationSet = Arrays.copyOfRange(trainingData, validationStart, validationEnd);

                            // 使用当前参数训练模型
                            HoltWintersForecast model = new HoltWintersForecast(alpha, beta, gamma,
                                    seasonLength, seasonalityType);
                            model.train(foldTrainSet);

                            // 预测验证集
                            double[] forecast = model.forecast(foldValidationSet.length);

                            // 计算此折的MAPE
                            double foldMAPE = calculateMAPE(foldValidationSet, forecast);
                            
                            // 排除异常值
                            if (!Double.isNaN(foldMAPE) && foldMAPE < 1000) {
                                totalMAPE += foldMAPE;
                                validFolds++;
                            }
                        }

                        // 计算平均MAPE
                        double averageMAPE = validFolds > 0 ? totalMAPE / validFolds : Double.MAX_VALUE;

                        // 更新最优参数
                        if (validFolds >= numFolds / 2 && averageMAPE < bestAverageMAPE) {
                            bestAverageMAPE = averageMAPE;
                            bestParams[0] = alpha;
                            bestParams[1] = beta;
                            bestParams[2] = gamma;
                        }
                    } catch (Exception e) {
                        // 忽略无效参数组合
                        System.err.println("无效参数组合: " +
                                alpha + ", " + beta + ", " + gamma + " - " + e.getMessage());
                    }
                }
            }
        }

        System.out.println("交叉验证最优参数: alpha=" + bestParams[0] +
                ", beta=" + bestParams[1] + ", gamma=" + bestParams[2] +
                ", 平均MAPE=" + bestAverageMAPE);

        return bestParams;
    }

    /**
     * 计算平均绝对百分比误差(MAPE)
     */
    public static double calculateMAPE(double[] actual, double[] forecast) {
        if (actual.length != forecast.length) {
            throw new IllegalArgumentException("实际值和预测值数组长度必须相同");
        }

        double sum = 0;
        int count = 0;

        for (int i = 0; i < actual.length; i++) {
            if (actual[i] != 0) {  // 避免除以零
                sum += Math.abs((actual[i] - forecast[i]) / actual[i]);
                count++;
            }
        }

        return (count > 0) ? (sum / count) * 100.0 : Double.NaN;
    }

    /**
     * 计算均方根误差(RMSE)
     */
    public static double calculateRMSE(double[] actual, double[] forecast) {
        if (actual.length != forecast.length) {
            throw new IllegalArgumentException("实际值和预测值数组长度必须相同");
        }

        double sumSquaredError = 0;

        for (int i = 0; i < actual.length; i++) {
            double error = actual[i] - forecast[i];
            sumSquaredError += error * error;
        }

        return Math.sqrt(sumSquaredError / actual.length);
    }

    /**
     * 计算平均绝对误差(MAE)，对零值和极小值更不敏感
     */
    public static double calculateMAE(double[] actual, double[] forecast) {
        if (actual.length != forecast.length) {
            throw new IllegalArgumentException("实际值和预测值数组长度必须相同");
        }

        double sumAbsError = 0;

        for (int i = 0; i < actual.length; i++) {
            sumAbsError += Math.abs(actual[i] - forecast[i]);
        }

        return sumAbsError / actual.length;
    }

    /**
     * 预测未来销量
     *
     * @param periods 要预测的期数
     * @return 预测结果数组
     */
    public double[] forecast(int periods) {
        if (periods <= 0) {
            return new double[0];
        }

        // 确保模型已训练
        if (level == 0 && seasonal[0] == 0) {
            throw new IllegalStateException("模型尚未训练");
        }

        double[] forecasts = new double[periods];

        for (int i = 0; i < periods; i++) {
            int season = (lastObservationIndex + i + 1) % seasonLength;

            if (seasonalityType == SeasonalityType.MULTIPLICATIVE) {
                forecasts[i] = (level + trend * (i + 1)) * seasonal[season];
            } else {
                forecasts[i] = level + trend * (i + 1) + seasonal[season];
            }

            // 确保预测值不为负
            if (forecasts[i] < 0) {
                forecasts[i] = 0;
            }
        }

        return forecasts;
    }

    /**
     * 获取模型评估指标
     *
     * @return 包含各项评估指标的数组(MAPE, RMSE, MAE)
     */
    public double[] getModelMetrics() {
        if (fittedValues == null || residuals == null) {
            throw new IllegalStateException("模型尚未训练");
        }

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
     * 获取当前模型参数
     *
     * @return 模型参数字符串
     */
    public String getModelParameters() {
        return String.format("模型类型: %s\nAlpha: %.4f, Beta: %.4f, Gamma: %.4f\n" +
                        "水平值: %.4f, 趋势值: %.4f\n季节性因子: %s",
                seasonalityType, alpha, beta, gamma,
                level, trend, Arrays.toString(seasonal));
    }

    /**
     * 获取拟合值
     */
    public double[] getFittedValues() {
        return fittedValues;
    }

    /**
     * 获取残差
     */
    public double[] getResiduals() {
        return residuals;
    }

    /**
     * 导出预测结果
     *
     * @param periods  预测期数
     * @param forecast 预测结果
     * @param labels   时间标签(可选)
     * @return 预测结果字符串
     */
    public static String exportForecast(int periods, double[] forecast, String[] labels) {
        StringBuilder sb = new StringBuilder();
        sb.append("周期,预测值\n");

        for (int i = 0; i < periods; i++) {
            String periodLabel = (labels != null && i < labels.length) ? labels[i] : String.valueOf(i + 1);
            sb.append(periodLabel)
              .append(",")
              .append(String.format("%.2f", forecast[i]))
              .append("\n");
        }

        return sb.toString();
    }

    /**
     * 便利方法：创建日期标签
     *
     * @param startYear  起始年份
     * @param startMonth 起始月份(1-12)
     * @param startDay   起始日(1-31)
     * @param periods    期数
     * @return 时间标签数组
     */
    public static String[] createDailyLabels(int startYear, int startMonth, int startDay, int periods) {
        String[] labels = new String[periods];

        // 简化版日期生成，不考虑月份天数变化等复杂情况
        for (int i = 0; i < periods; i++) {
            int day = startDay + i;
            int month = startMonth;
            int year = startYear;

            // 简单处理月份进位(忽略闰年和不同月份天数)
            while (day > 30) {
                day -= 30;
                month++;
            }

            while (month > 12) {
                month -= 12;
                year++;
            }

            labels[i] = year + "-" + String.format("%02d", month) + "-" + String.format("%02d", day);
        }

        return labels;
    }

    /**
     * 便捷方法：自动选择最优参数并创建模型（针对日度数据优化参数范围）
     *
     * @param data            历史数据
     * @param seasonLength    季节长度
     * @param seasonalityType 季节性类型
     * @return 优化后的模型
     */
    public static HoltWintersForecast createOptimizedModel(double[] data, int seasonLength,
                                                           SeasonalityType seasonalityType) {
        // 针对日度数据优化的参数网格
        double[] alphaValues = {0.01, 0.03, 0.05, 0.08, 0.1, 0.15, 0.2};
        double[] betaValues = {0.0, 0.01, 0.02, 0.05, 0.08, 0.1};
        double[] gammaValues = {0.01, 0.03, 0.05, 0.08, 0.1, 0.15};

        // 使用交叉验证查找最优参数
        double[] optimalParams = findOptimalParametersWithCV(data, alphaValues, betaValues, gammaValues,
                seasonLength, seasonalityType);

        // 创建并训练模型
        HoltWintersForecast model = new HoltWintersForecast(
                optimalParams[0], optimalParams[1], optimalParams[2], seasonLength, seasonalityType);
        model.train(data);

        return model;
    }

    /**
     * 对原始数据进行七日移动平均处理
     *
     * @param rawData 原始数据
     * @return 处理后的数据
     */
    public static double[] applyMovingAverage(double[] rawData) {
        int windowSize = 14;
        // 参数校验
        if (rawData == null || rawData.length == 0 || windowSize < 1) {
            return new double[0];
        }

        // 特殊处理窗口大小为1的情况
        if (windowSize == 1) {
            return Arrays.copyOf(rawData, rawData.length);
        }

        double[] smoothedData = new double[rawData.length];
        final int maxInitialDays = Math.min(windowSize - 1, rawData.length);

        // 前N-1天使用累积平均
        for (int i = 0; i < maxInitialDays; i++) {
            double sum = 0;
            for (int j = 0; j <= i; j++) {
                sum += rawData[j];
            }
            smoothedData[i] = sum / (i + 1);
        }

        // 从第N天开始使用完整窗口平均
        for (int i = windowSize - 1; i < rawData.length; i++) {
            double sum = 0;
            // 计算窗口内数据的和
            for (int j = i - windowSize + 1; j <= i; j++) {
                sum += rawData[j];
            }
            smoothedData[i] = sum / windowSize;
        }

        return smoothedData;
    }

    /**
     * 判断是否应使用季节性模型
     *
     * @param data              历史数据
     * @param dailySeasonLength 日度季节周期长度(通常是365)
     * @return 是否应使用季节性模型
     */
    public static boolean shouldUseSeasonalModel(double[] data, int dailySeasonLength) {
        // 仅当数据量超过一年时才使用季节性模型
        return data.length >= dailySeasonLength;
    }

    /**
     * 为Holt双指数平滑方法寻找最优参数
     *
     * @param trainingData 训练数据
     * @return 最优[alpha, beta]参数数组
     */
    public static double[] findOptimalHoltParameters(double[] trainingData) {
        if (trainingData.length < 15) { // 至少需要30天数据进行优化
            throw new IllegalArgumentException("Holt参数优化需要至少30天数据");
        }

        // 将数据分为训练集和验证集(80%训练，20%验证)
        int validationStart = (int) (trainingData.length * 0.8);
        double[] trainSet = Arrays.copyOfRange(trainingData, 0, validationStart);

        double[] validationSet = Arrays.copyOfRange(trainingData, Math.max(10, validationStart), trainingData.length);


        double[] alphaValues = IntStream.rangeClosed(1, 100)
                                        .mapToDouble(v -> v * 0.01)
                                        .toArray();
        double[] betaValues = IntStream.rangeClosed(1, 100)
                                       .mapToDouble(v -> v * 0.01)
                                       .toArray();
        double bestMAE = Double.MAX_VALUE;  // 使用MAE代替MAPE，对零值和小值更友好
        double[] bestParams = new double[2];

        // 网格搜索
        for (double alpha : alphaValues) {
            for (double beta : betaValues) {
                try {
                    // 使用当前参数进行预测
                    double[] forecast = forecastWithHoltMethod(trainSet, validationSet.length, alpha, beta);

                    // 计算MAE
                    double mae = calculateMAE(validationSet, forecast);
                    double rmse = calculateRMSE(validationSet, forecast);
                    double a = rmse / mae;
                    // 更新最优参数
                    if (mae < bestMAE&& a<1.5) {
                        bestMAE = mae;
                        bestParams[0] = alpha;
                        bestParams[1] = beta;
                    }
                } catch (Exception e) {
                    // 忽略无效参数组合
                    System.err.println("无效Holt参数组合: " + alpha + ", " + beta + " - " + e.getMessage());
                }
            }
        }

        System.out.println("Holt最优参数: alpha=" + bestParams[0] +
                ", beta=" + bestParams[1] + ", MAE=" + bestMAE);

        return bestParams;
    }

    /**
     * 为Holt双指数平滑方法寻找最优参数(使用交叉验证)
     * 
     * @param trainingData 训练数据
     * @return 最优[alpha, beta]参数数组
     */
    public static double[] findOptimalHoltParametersWithCV(double[] trainingData) {
        if (trainingData.length < 15) { // 至少需要30天数据
            throw new IllegalArgumentException("Holt参数优化需要至少30天数据");
        }

        // 设置交叉验证参数
        int minTrainSize = Math.max(15, trainingData.length / 3); // 最小训练集大小
        int windowSize = Math.max(5, trainingData.length / 10); // 验证窗口大小
        int step = Math.max(1, windowSize / 2); // 滑动步长
        int numFolds = (trainingData.length - minTrainSize - windowSize) / step + 1;
        
        // 限制折数
        numFolds = Math.min(numFolds, 10);
        numFolds = Math.max(numFolds, 3); // 至少3折
        
        System.out.println("执行" + numFolds + "折时间序列交叉验证(Holt方法)...");

        // 参数网格可以减少密度以提高速度
        double[] alphaValues = new double[20]; // 减少到20个点
        double[] betaValues = new double[20];  // 减少到20个点
        
        for (int i = 0; i < 20; i++) {
            alphaValues[i] = (i + 1) * 0.05; // 0.05到1.0, 步长0.05
            betaValues[i] = i * 0.05;        // 0.0到0.95, 步长0.05
        }

        double bestMAE = Double.MAX_VALUE;
        double[] bestParams = new double[2];

        // 网格搜索
        for (double alpha : alphaValues) {
            for (double beta : betaValues) {
                try {
                    double totalMAE = 0;
                    int validFolds = 0;

                    // 对每个交叉验证折进行评估
                    for (int fold = 0; fold < numFolds; fold++) {
                        int validationStart = minTrainSize + fold * step;
                        int validationEnd = Math.min(validationStart + windowSize, trainingData.length);
                        
                        if (validationEnd - validationStart < 3) { // 验证集至少需要3个点
                            continue;
                        }

                        double[] foldTrainSet = Arrays.copyOfRange(trainingData, 0, validationStart);
                        double[] foldValidationSet = Arrays.copyOfRange(trainingData, validationStart, validationEnd);

                        // 使用当前参数进行预测
                        double[] forecast = forecastWithHoltMethod(foldTrainSet, foldValidationSet.length, alpha, beta);

                        // 计算MAE
                        double foldMAE = calculateMAE(foldValidationSet, forecast);
                        
                        // 计算RMSE用于筛选异常比例
                        double foldRMSE = calculateRMSE(foldValidationSet, forecast);
                        double rmseToMaeRatio = foldRMSE / foldMAE;
                        
                        // 只有当RMSE/MAE比例合理时才计入总误差
                        if (rmseToMaeRatio < 1.5) {
                            totalMAE += foldMAE;
                            validFolds++;
                        }
                    }

                    // 计算平均MAE
                    double averageMAE = validFolds > 0 ? totalMAE / validFolds : Double.MAX_VALUE;

                    // 更新最优参数
                    if (validFolds >= numFolds / 2 && averageMAE < bestMAE) {
                        bestMAE = averageMAE;
                        bestParams[0] = alpha;
                        bestParams[1] = beta;
                    }
                } catch (Exception e) {
                    // 忽略无效参数组合
                    System.err.println("无效Holt参数组合: " + alpha + ", " + beta + " - " + e.getMessage());
                }
            }
        }

        System.out.println("Holt交叉验证最优参数: alpha=" + bestParams[0] +
                ", beta=" + bestParams[1] + ", 平均MAE=" + bestMAE);

        return bestParams;
    }

    /**
     * 使用双指数平滑(Holt)方法预测(无季节性)
     *
     * @param data    历史数据
     * @param periods 预测期数
     * @return 预测结果
     */
    public static double[] forecastWithHoltMethod(double[] data, int periods) {
        if (data == null || data.length < 2) {
            throw new IllegalArgumentException("Holt预测至少需要2个数据点");
        }

        // 寻找最优参数(使用交叉验证)
        double[] optimalParams = findOptimalHoltParametersWithCV(data);

        double alpha = optimalParams[0];
        double beta = optimalParams[1];

        return forecastWithHoltMethod(data, periods, alpha, beta);
    }

    /**
     * 使用双指数平滑(Holt)方法预测(无季节性)，使用指定参数
     *
     * @param data    历史数据
     * @param periods 预测期数
     * @param alpha   水平平滑系数
     * @param beta    趋势平滑系数
     * @return 预测结果
     */
    public static double[] forecastWithHoltMethod(double[] data, int periods, double alpha, double beta) {
        if (data == null || data.length < 2) {
            throw new IllegalArgumentException("Holt预测至少需要2个数据点");
        }

        // 初始化水平和趋势
        double level = data[0];
        double trend = data.length > 1 ? data[1] - data[0] : 0;

        // 根据历史数据更新水平和趋势
        for (int i = 1; i < data.length; i++) {
            double prevLevel = level;
            level = alpha * data[i] + (1 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1 - beta) * trend;
        }

        // 预测
        double[] forecast = new double[periods];
        for (int i = 0; i < periods; i++) {
            forecast[i] = level + (i + 1) * trend;
            if (forecast[i] < 0) forecast[i] = 0; // 确保预测值非负
        }

        return forecast;
    }

    /**
     * 日度预测的统一入口
     *
     * @param rawData      原始历史数据
     * @param forecastDays 预测天数
     * @return 预测结果
     */
    public static double[] dailyForecast(double[] rawData, int forecastDays) {
        if (rawData == null || rawData.length == 0) {
            throw new IllegalArgumentException("历史数据不能为空");
        }

        // 1. 应用七日移动平均平滑处理
        // double[] smoothedData = applyMovingAverage(rawData);
        double[] smoothedData = rawData;

        // 2. 判断是否使用季节性模型
        int dailySeasonLength = 365; // 年度季节性
        boolean useSeasonalModel = shouldUseSeasonalModel(smoothedData, dailySeasonLength);

        // 3. 选择合适的预测方法
        if (useSeasonalModel) {
            System.out.println("数据长度超过一年，使用Holt-Winters三重指数平滑(季节性)模型");

            try {
                // 对于日度数据推荐使用加法模型，尤其是零值较多的情况
                HoltWintersForecast model = createOptimizedModel(
                        smoothedData, dailySeasonLength, SeasonalityType.ADDITIVE);

                // 输出模型评估指标
                double[] metrics = model.getModelMetrics();
                System.out.println("Holt-Winters模型评估：MAPE=" + metrics[0] +
                        ", RMSE=" + metrics[1] + ", MAE=" + metrics[2]);

                return model.forecast(forecastDays);
            } catch (Exception e) {
                // 如果季节性模型失败，降级到非季节性模型
                System.out.println("季节性模型失败，原因: " + e.getMessage());
                System.out.println("降级使用Holt双指数平滑模型");
                return forecastWithHoltMethod(smoothedData, forecastDays);
            }
        } else {
            System.out.println("数据长度不足一年，使用Holt双指数平滑(非季节性)模型");
            return forecastWithHoltMethod(smoothedData, forecastDays);
        }
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {

        // 演示日度销售数据预测
        System.out.println("=============== 日度销售数据预测示例 ===============");

        // 一年零两个月的日度销售数据示例(简化为随机数据)
        double[] dailySalesHistory = new double[]{8.0, 2.0, 8.0, 6.0, 11.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 4.0, 3.0, 0.0, 2.0, 0.0, 1.0, 5.0, 3.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, 9.0, 2.0, 12.0, 9.0, 3.0, 4.0, 0.0, 2.0, 5.0, 6.0, 5.0, 4.0, 5.0, 6.0, 11.0, 2.0, 13.0, 7.0, 4.0, 3.0, 5.0, 13.0, 8.0, 2.0, 11.0, 4.0, 0.0, 6.0, 13.0, 6.0, 8.0, 9.0, 21.0, 1.0, 2.0, 1.0, 5.0, 11.0, 0.0, 2.0, 23.0, 9.0, 7.0, 13.0, 12.0, 1.0, 9.0, 2.0, 0.0, 6.0, 30.0, 6.0, 12.0, 1.0, 6.0, 5.0, 4.0, 1.0, 21.0, 16.0, 14.0, 1.0, 22.0, 5.0, 8.0, 8.0, 6.0, 9.0, 7.0, 5.0, 19.0, 20.0, 30.0, 12.0, 11.0, 3.0, 16.0, 4.0, 0.0, 1.0, 8.0, 23.0, 0.0, 36.0, 12.0, 2.0, 9.0, 0.0, 18.0, 13.0, 6.0, 7.0, 21.0, 0.0, 3.0, 13.0, 1.0, 25.0, 24.0, 0.0, 3.0, 13.0, 4.0, 9.0, 20.0, 0.0, 29.0, 17.0, 19.0, 5.0, 0.0, 18.0, 8.0, 10.0, 0.0, 12.0, 21.0, 10.0, 8.0, 18.0, 0.0, 4.0, 8.0, 0.0, 6.0, 6.0, 7.0, 22.0, 0.0, 0.0, 7.0, 2.0, 16.0, 7.0, 7.0, 18.0, 26.0, 2.0, 13.0, 4.0, 20.0, 8.0, 6.0, 5.0, 1.0, 12.0, 7.0, 0.0, 12.0, 31.0, 0.0, 18.0, 15.0, 19.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 30.0, 25.0, 11.0, 11.0, 6.0, 9.0, 15.0, 4.0, 23.0, 0.0, 13.0, 6.0, 14.0, 27.0, 5.0, 2.0, 13.0, 3.0, 0.0, 1.0, 14.0, 0.0, 16.0, 5.0, 15.0, 0.0, 14.0, 7.0, 10.0, 13.0, 21.0, 8.0, 4.0, 7.0, 13.0, 28.0, 19.0, 0.0, 10.0, 2.0, 14.0, 14.0, 9.0, 2.0, 3.0, 0.0, 9.0, 4.0, 0.0, 4.0, 11.0, 25.0, 20.0, 13.0, 5.0, 17.0, 7.0, 10.0, 3.0, 3.0, 21.0, 3.0, 8.0, 10.0, 2.0, 27.0, 20.0, 13.0, 11.0, 12.0, 0.0, 10.0, 10.0, 5.0, 5.0, 8.0, 12.0, 5.0, 10.0, 16.0, 12.0, 0.0, 13.0, 7.0, 3.0, 15.0, 5.0, 0.0, 0.0, 10.0, 17.0, 9.0, 2.0, 21.0, 3.0, 8.0, 0.0, 0.0, 10.0, 3.0, 13.0, 7.0, 9.0, 13.0, 15.0, 0.0, 0.0, 14.0, 12.0, 15.0, 11.0, 5.0, 0.0, 0.0, 11.0, 13.0, 9.0, 1.0, 1.0, 7.0, 0.0, 4.0, 15.0, 3.0, 29.0, 0.0, 17.0, 3.0, 8.0, 14.0, 2.0, 17.0, 14.0, 2.0, 8.0, 3.0, 0.0, 5.0, 14.0, 7.0, 4.0, 5.0, 5.0, 12.0, 8.0, 1.0, 11.0, 0.0, 3.0, 4.0, 1.0, 5.0, 4.0, 0.0, 5.0, 6.0, 0.0, 12.0, 7.0, 0.0, 24.0, 8.0, 7.0, 11.0, 3.0, 2.0, 4.0, 12.0, 0.0, 0.0, 0.0, 3.0, 14.0, 9.0, 4.0, 2.0, 19.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 7.0, 0.0, 15.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 7.0, 2.0, 8.0, 0.0, 8.0, 0.0, 0.0, 1.0, 6.0}; // 一年零两个月
        // 3个月的日度销售数据
        double[] shortTermData = Arrays.copyOfRange(dailySalesHistory, dailySalesHistory.length - 100, dailySalesHistory.length);
        System.out.println(Arrays.toString(shortTermData));

        // 预测未来30天销量
        int futureDays = 30;
        double[] futureSales = dailyForecast(shortTermData, futureDays);

        // 创建时间标签(假设从2023年1月1日开始)
        String[] forecastLabels = createDailyLabels(2024, 1, 1 + shortTermData.length, futureDays);

        // 输出预测结果
        System.out.println("\n未来30天销量预测结果:");
        System.out.println(exportForecast(futureDays, futureSales, forecastLabels));


    }
}
