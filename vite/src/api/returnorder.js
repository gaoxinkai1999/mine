import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 查询退货订单列表（旧API）
   * @url /returnOrder/list
   * @method GET
   * @param {Object} params Query parameters
   * @param {number} params.shopId 
   * @param {string} params.startDate 
   * @param {string} params.endDate 
   * @returns {Promise<any>}
   */
  getReturnOrdersLegacy(params = {}) {
    return request({
      url: '/returnOrder/list',
      method: 'GET',
      params
    })
  },

  /**
   * @description 分页查询退货订单列表
   * @url /returnOrder/list
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  getReturnOrders(data) {
    return request({
      url: '/returnOrder/list',
      method: 'POST',
      data
    })
  },

  /**
   * @description 删除退货订单
   * @url /returnOrder/delete/{id}
   * @method POST
   * @param {number} id 
   * @returns {Promise<any>}
   */
  deleteReturnOrder(id) {
    return request({
      url: `/returnOrder/delete/${id}`,
      method: 'POST'
    })
  },

  /**
   * @description 创建退货订单
   * @url /returnOrder/create
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  createReturnOrder(data) {
    return request({
      url: '/returnOrder/create',
      method: 'POST',
      data
    })
  },

  /**
   * @description 根据ID查询退货订单
   * @url /returnOrder/{id}
   * @method GET
   * @param {number} id 
   * @returns {Promise<any>}
   */
  getReturnOrderById(id) {
    return request({
      url: `/returnOrder/${id}`,
      method: 'GET'
    })
  }
}

export default api