import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 批量更新库存信息
   * @url /inventory/batch-update
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  batchUpdate(data) {
    return request({
      url: '/inventory/batch-update',
      method: 'POST',
      data
    })
  }
}

export default api