import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 获取临期批次列表
   * @url /api/monitor/expiry/batches
   * @method GET

   * @returns {Promise<any>}
   */
  getNearExpiryBatches() {
    return request({
      url: '/api/monitor/expiry/batches',
      method: 'GET'
    })
  }
}

export default api