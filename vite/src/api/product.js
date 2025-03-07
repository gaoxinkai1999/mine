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
   * @description 获取店铺可售商品列表（包含库存信息）
   * @url /product/sale-list
   * @method GET
   * @param {Object} params Query parameters
   * @param {number} params.shopId 
   * @returns {Promise<any>}
   */
  getProductSaleList(params = {}) {
    return request({
      url: '/product/sale-list',
      method: 'GET',
      params
    })
  }
}

export default api