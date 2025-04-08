import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 查询批次列表
   * @url /batch/list
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  getBatches(data) {
    return request({
      url: '/batch/list',
      method: 'POST',
      data
    })
  },

  /**
   * @description 批量更新批次信息
   * @url /batch/batch-update
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  batchUpdate(data) {
    return request({
      url: '/batch/batch-update',
      method: 'POST',
      data
    })
  },

  /**
   * @description 查询商品所有批次
   * @url /batch/product/{productId}
   * @method GET
   * @param {number} productId 
   * @param {Object} params Query parameters
   * @param {boolean} params.status 
   * @returns {Promise<any>}
   */
  getProductBatches(productId, params = {}) {
    return request({
      url: `/batch/product/${productId}`,
      method: 'GET',
      params
    })
  },

  /**
   * @description 查询商品有效批次
   * @url /batch/product/{productId}/valid
   * @method GET
   * @param {number} productId 
   * @returns {Promise<any>}
   */
  getValidBatches(productId) {
    return request({
      url: `/batch/product/${productId}/valid`,
      method: 'GET'
    })
  }
}

export default api