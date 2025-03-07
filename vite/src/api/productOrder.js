import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 获取产品销售订单项列表
   * @url /product-order/list
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  getProductOrderList(data) {
    return request({
      url: '/product-order/list',
      method: 'POST',
      data
    })
  }
}

export default api