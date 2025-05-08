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
  },

  /**
   * @description 将非批次管理商品转换为批次管理商品
   * @url /product/convertToBatchProduct
   * @method POST
   * @param {Object} params Query parameters
   * @param {number} params.productId 商品ID
   * @param {string} [params.productionDate] 生产日期，可选，默认为当天
   * @returns {Promise<any>}
   */
  convertToBatchProduct(params = {}) {
    return request({
      url: '/product/convertToBatchProduct',
      method: 'POST',
      params
    })
  }
}

export default api