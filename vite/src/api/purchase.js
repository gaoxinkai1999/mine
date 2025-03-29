import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 删除采购订单
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
   * @description 获取采购订单列表（支持分页查询）
   * @url /purchase/list
   * @method POST
   * @param {Object} data Request body
   * @param {number} data.page 页码，从0开始
   * @param {number} data.size 每页大小
   * @param {string} [data.state] 状态过滤
   * @param {string} [data.createTimeStart] 创建开始时间
   * @param {string} [data.createTimeEnd] 创建结束时间
   * @param {number} [data.productId] 商品ID
   * @returns {Promise<any>}
   */
  getPurchaseList(data = {}) {
    return request({
      url: '/purchase/list',
      method: 'POST',
      data
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
  },

  /**
   * @description 处理采购单入库
   * @url /purchase/in-stock
   * @method POST
   * @param {Object} data Request body
   * @param {number} data.purchaseId 采购单ID
   * @param {Array} data.batchInfoList 批次信息列表
   * @returns {Promise<any>}
   */
  processPurchaseInStock(data) {
    return request({
      url: '/purchase/in-stock',
      method: 'POST',
      data
    })
  }
}

export default api