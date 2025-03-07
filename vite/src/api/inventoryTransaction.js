import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 查询库存变动记录
   * @url /inventory/transaction/list
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  getTransactions(data) {
    return request({
      url: '/inventory/transaction/list',
      method: 'POST',
      data
    })
  },

  /**
   * @description 获取商品库存变动汇总
   * @url /inventory/transaction/product/{productId}/summary
   * @method GET
   * @param {number} productId 
   * @param {Object} params Query parameters
   * @param {string} params.startTime 
   * @param {string} params.endTime 
   * @returns {Promise<any>}
   */
  getTransactionSummary(productId, params = {}) {
    return request({
      url: `/inventory/transaction/product/${productId}/summary`,
      method: 'GET',
      params
    })
  },

  /**
   * @description 获取最近库存变动记录
   * @url /inventory/transaction/product/{productId}/recent
   * @method GET
   * @param {number} productId 
   * @param {Object} params Query parameters
   * @param {number} params.limit 
   * @returns {Promise<any>}
   */
  getRecentTransactions(productId, params = {}) {
    return request({
      url: `/inventory/transaction/product/${productId}/recent`,
      method: 'GET',
      params
    })
  },

  /**
   * @description 查询商品库存变动历史
   * @url /inventory/transaction/product/{productId}/history
   * @method GET
   * @param {number} productId 
   * @param {Object} params Query parameters
   * @param {string} params.startTime 
   * @param {string} params.endTime 
   * @returns {Promise<any>}
   */
  getTransactionHistory(productId, params = {}) {
    return request({
      url: `/inventory/transaction/product/${productId}/history`,
      method: 'GET',
      params
    })
  },

  /**
   * @description 查询批次库存变动历史
   * @url /inventory/transaction/batch/{batchId}/history
   * @method GET
   * @param {number} batchId 
   * @returns {Promise<any>}
   */
  getBatchTransactionHistory(batchId) {
    return request({
      url: `/inventory/transaction/batch/${batchId}/history`,
      method: 'GET'
    })
  }
}

export default api