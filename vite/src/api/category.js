import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 修改品类
   * @url /category/update
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  update(data) {
    return request({
      url: '/category/update',
      method: 'POST',
      data
    })
  },

  /**
   * @description 
   * @url /category/deleteCategory
   * @method POST
   * @param {Object} params Query parameters
   * @param {number} params.categoryId 
   * @returns {Promise<any>}
   */
  deleteCategory(params = {}) {
    return request({
      url: '/category/deleteCategory',
      method: 'POST',
      params
    })
  },

  /**
   * @description 新建品类
   * @url /category/create
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  create(data) {
    return request({
      url: '/category/create',
      method: 'POST',
      data
    })
  },

  /**
   * @description 批量更新类别信息
   * @url /category/batch-update
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  batchUpdate(data) {
    return request({
      url: '/category/batch-update',
      method: 'POST',
      data
    })
  },

  /**
   * @description 查询所有品类
   * @url /category/list
   * @method GET

   * @returns {Promise<any>}
   */
  getCategories() {
    return request({
      url: '/category/list',
      method: 'GET'
    })
  }
}

export default api