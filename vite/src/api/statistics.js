import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 获取产品移动平均线数据
   * @url /statistics/products
   * @method POST
   * @param {Object} params Query parameters
   * @param {string} params.startDate 
   * @param {string} params.endDate 
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  getMovingAverage(params = {}, data) {
    return request({
      url: '/statistics/products',
      method: 'POST',
      params,
      data
    })
  },

  /**
   * @description 获取商家统计数据
   * @url /statistics/shop
   * @method GET

   * @returns {Promise<any>}
   */
  getShopStatistics() {
    return request({
      url: '/statistics/shop',
      method: 'GET'
    })
  },

  /**
   * @description 获取总体趋势数据
   * @url /statistics/overall-trend
   * @method GET
   * @param {Object} params Query parameters
   * @param {number} params.period 
   * @param {string} params.startDate 
   * @param {string} params.endDate 
   * @returns {Promise<any>}
   */
  getOverallTrend(params = {}) {
    return request({
      url: '/statistics/overall-trend',
      method: 'GET',
      params
    })
  },

  /**
   * @description 获取月度统计数据
   * @url /statistics/monthly
   * @method GET
   * @param {Object} params Query parameters
   * @param {string} params.startDate 
   * @param {string} params.endDate 
   * @returns {Promise<any>}
   */
  getMonthlyStatistics(params = {}) {
    return request({
      url: '/statistics/monthly',
      method: 'GET',
      params
    })
  },

  /**
   * @description 获取日期范围统计数据
   * @url /statistics/date-range
   * @method GET
   * @param {Object} params Query parameters
   * @param {string} params.startDate 
   * @param {string} params.endDate 
   * @returns {Promise<any>}
   */
  getDateRangeStatistics(params = {}) {
    return request({
      url: '/statistics/date-range',
      method: 'GET',
      params
    })
  },

  /**
   * @description 获取每日统计数据
   * @url /statistics/daily
   * @method GET
   * @param {Object} params Query parameters
   * @param {string} params.startDate 
   * @param {string} params.endDate 
   * @returns {Promise<any>}
   */
  getDailyStatistics(params = {}) {
    return request({
      url: '/statistics/daily',
      method: 'GET',
      params
    })
  }
}

export default api