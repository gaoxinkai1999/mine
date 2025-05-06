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
  },
  /**
   * @description 标记临期商品条目为已处理
   * @url /api/monitor/expiry/items/{saleBatchDetailId}/process
   * @method POST
   * @param {number} saleBatchDetailId
   * @returns {Promise<any>}
   */
  markExpiryItemAsProcessed(saleBatchDetailId) {
    return request({
      url: `/api/monitor/expiry/items/${saleBatchDetailId}/process`,
      method: 'POST'
    })
  },
  /**
   * @description 获取已处理的临期商品历史列表
   * @url /api/monitor/expiry/processed-items
   * @method GET
   * @param {object} params - 分页参数 { page, size }
   * @returns {Promise<any>}
   */
  getProcessedItemHistory(params) {
    return request({
      url: '/api/monitor/expiry/processed-items',
      method: 'GET',
      params: {
        ...params,
        _t: new Date().getTime() // 添加时间戳参数以防止缓存
      }
    })
  },
  /**
   * @description 撤销临期商品条目的处理标记
   * @url /api/monitor/expiry/items/{saleBatchDetailId}/unprocess
   * @method POST
   * @param {number} saleBatchDetailId
   * @returns {Promise<any>}
   */
  unmarkExpiryItemAsProcessed(saleBatchDetailId) {
    return request({
      url: `/api/monitor/expiry/items/${saleBatchDetailId}/unprocess`,
      method: 'POST'
    })
  }
}

export default api