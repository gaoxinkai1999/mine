import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 查询所有商品
   * @url /product/getProducts
   * @method POST

   * @returns {Promise<any>}
   */
  getProducts() {
    return request({
      url: '/product/getProducts',
      method: 'POST'
    })
  },

  /**
   * @description 
   * @url /product/deleteProduct
   * @method POST
   * @param {Object} params Query parameters
   * @param {number} params.productId 
   * @returns {Promise<any>}
   */
  deleteProduct(params = {}) {
    return request({
      url: '/product/deleteProduct',
      method: 'POST',
      params
    })
  },

  /**
   * @description 新建商品
   * @url /product/createProduct
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  createProduct(data) {
    return request({
      url: '/product/createProduct',
      method: 'POST',
      data
    })
  },

  /**
   * @description 批量更新产品信息
   * @url /product/batch-update
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  batchUpdate(data) {
    return request({
      url: '/product/batch-update',
      method: 'POST',
      data
    })
  }
}

export default api