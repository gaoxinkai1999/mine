import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 分页查询采购订单列表
   * @url /purchase/list
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  getPurchaseList(data) {
    return request({
      url: '/purchase/list',
      method: 'POST',
      data
    })
  },

  /**
   * @description 采购单入库
   * @url /purchase/in-stock
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  processPurchaseInStock(data) {
    return request({
      url: '/purchase/in-stock',
      method: 'POST',
      data
    })
  },

  /**
   * @description 创建采购订单
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
   * @description 取消采购订单
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
   * @description 获取包含采购信息的在售商品列表
   * @url /purchase/getOnSaleProductsWithPurchaseInfo
   * @method GET

   * @returns {Promise<any>}
   */
  getOnSaleProductsWithPurchaseInfo() {
    return request({
      url: '/purchase/getOnSaleProductsWithPurchaseInfo',
      method: 'GET'
    })
  },

  /**
   * @description 获取采购订单详情
   * @url /purchase/detail
   * @method GET
   * @param {Object} params Query parameters
   * @param {number} params.id 
   * @returns {Promise<any>}
   */
  getPurchaseDetail(params = {}) {
    return request({
      url: '/purchase/detail',
      method: 'GET',
      params
    })
  }
}

export default api