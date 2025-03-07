import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 获取产品移动平均线数据
   * @url /statistics/products
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  getMovingAverage(data) {
    return request({
      url: '/statistics/products',
      method: 'POST',
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
  },

  /**
   * @description 
   * @url /statistics/daily-sales/{productId}
   * @method GET
   * @param {number} productId 
   * @returns {Promise<any>}
   */
  getDailySales(productId) {
    return request({
      url: `/statistics/daily-sales/${productId}`,
      method: 'GET'
    })
  }
}

export default api