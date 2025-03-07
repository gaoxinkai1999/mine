import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 
   * @url /purchase/delete
   * @method POST
   * @param {Object} params Query parameters
   * @param {number} params.id 
   * @returns {Promise<any>}
   */
  delete(params = {}) {
    return request({
      url: '/purchase/delete',
      method: 'POST',
      params
    })
  },

  /**
   * @description 
   * @url /purchase/create
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  createPurchaseOrder(data) {
    return request({
      url: '/purchase/create',
      method: 'POST',
      data
    })
  },

  /**
   * @description 
   * @url /purchase/cancel
   * @method POST
   * @param {Object} params Query parameters
   * @param {number} params.purchaseId 
   * @returns {Promise<any>}
   */
  cancelPurchaseOrder(params = {}) {
    return request({
      url: '/purchase/cancel',
      method: 'POST',
      params
    })
  },

  /**
   * @description 获取商品采购建议（基于历史平均值）
   * @url /purchase/suggestions
   * @method GET
   * @param {Object} params Query parameters
   * @param {number} params.daysToAnalyze 分析的历史天数
   * @param {number} params.leadTimeDays 补货周期天数
   * @param {number} params.safetyStockDays 安全库存天数
   * @returns {Promise<any>}
   */
  getPurchaseSuggestions(params = {}) {
    return request({
      url: '/purchase/suggestions',
      method: 'GET',
      params
    })
  }
}

export default api