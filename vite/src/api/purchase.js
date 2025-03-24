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
   * @description 
   * @url /purchase/getOnSaleProductsWithPurchaseInfo
   * @method GET

   * @returns {Promise<any>}
   */
  getOnSaleProductsWithPurchaseInfo() {
    return request({
      url: '/purchase/getOnSaleProductsWithPurchaseInfo',
      method: 'GET'
    })
  }
}

export default api