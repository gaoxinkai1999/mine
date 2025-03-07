import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 获取销售预测概要
   * @url /api/forecast/summary
   * @method GET

   * @returns {Promise<any>}
   */
  getForecastSummary() {
    return request({
      url: '/api/forecast/summary',
      method: 'GET'
    })
  },

  /**
   * @description 获取销售预测
   * @url /api/forecast/sales
   * @method GET
   * @param {Object} params Query parameters
   * @param {number} params.days 预测天数
   * @param {number} params.historyDays 使用的历史天数
   * @returns {Promise<any>}
   */
  getForecast(params = {}) {
    return request({
      url: '/api/forecast/sales',
      method: 'GET',
      params
    })
  },

  /**
   * @description 获取指定日期范围的销售预测
   * @url /api/forecast/sales/range
   * @method GET
   * @param {Object} params Query parameters
   * @param {string} params.startDate 开始日期
   * @param {string} params.endDate 结束日期
   * @returns {Promise<any>}
   */
  getForecastByDateRange(params = {}) {
    return request({
      url: '/api/forecast/sales/range',
      method: 'GET',
      params
    })
  },

  /**
   * @description 获取产品销售预测
   * @url /api/forecast/product/{productId}
   * @method GET
   * @param {number} productId 产品ID
   * @param {Object} params Query parameters
   * @param {number} params.days 预测天数
   * @returns {Promise<any>}
   */
  getProductForecast(productId, params = {}) {
    return request({
      url: `/api/forecast/product/${productId}`,
      method: 'GET',
      params
    })
  }
}

export default api