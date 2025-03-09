import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 修改库存
   * @url /inventory/update
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  update(data) {
    return request({
      url: '/inventory/update',
      method: 'POST',
      data
    })
  }
}

export default api