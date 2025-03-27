package com.example.domain.forecast.strategy;

import com.example.exception.MyException;

/**
 * 预测策略接口
 * 定义了所有预测算法需要遵循的规范。
 */
public interface ForecastStrategy {

    /**
     * 执行预测。
     *
     * @param historicalData 经过预处理（如平滑）的历史数据。
     * @param forecastDays   需要预测的未来天数。
     * @return 包含未来每天预测值的数组。
     * @throws MyException 如果预测过程中发生错误。
     */
    double[] forecast(double[] historicalData, int forecastDays) throws MyException;

    /**
     * 判断该策略是否能够处理给定长度的历史数据。
     *
     * @param dataLength 历史数据的点数。
     * @return 如果该策略适用，则返回 true；否则返回 false。
     */
    boolean canHandle(int dataLength);

    /**
     * 获取策略的名称，主要用于日志记录和调试。
     *
     * @return 策略的易读名称。
     */
    String getStrategyName();

    /**
     * 获取该策略适用的最小数据长度。
     * 这个值用于对策略进行排序，以便优先选择能处理更多数据的（通常更复杂）策略。
     *
     * @return 适用的最小数据点数量。
     */
    int getMinDataLength();
}
