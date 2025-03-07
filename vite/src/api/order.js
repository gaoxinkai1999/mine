import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 分页查询订单列表
   * @url /order/list
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  getOrders(data) {
    return request({
      url: '/order/list',
      method: 'POST',
      data
    })
  },

  /**
   * @description 新建订单
   * @url /order/create
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  createOrder(data) {
    return request({
      url: '/order/create',
      method: 'POST',
      data
    })
  },

  /**
   * @description 
   * @url /order/cancel
   * @method POST
   * @param {Object} params Query parameters
   * @param {number} params.orderId 
   * @returns {Promise<any>}
   */
  cancelOrder(params = {}) {
    return request({
      url: '/order/cancel',
      method: 'POST',
      params
    })
  }
}

export default api