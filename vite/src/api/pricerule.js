import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 获取简易价格规则列表
   * @url /priceRule/getSimplePriceRules
   * @method GET

   * @returns {Promise<any>}
   */
  getSimplePriceRules() {
    return request({
      url: '/priceRule/getSimplePriceRules',
      method: 'GET'
    })
  },

  /**
   * @description 获取价格规则列表
   * @url /priceRule/getPriceRules
   * @method GET

   * @returns {Promise<any>}
   */
  getPriceRules() {
    return request({
      url: '/priceRule/getPriceRules',
      method: 'GET'
    })
  },

  /**
   * @description 查询单个价格规则
   * @url /priceRule/getPriceRule/{id}
   * @method GET
   * @param {number} id 
   * @returns {Promise<any>}
   */
  getPriceRule(id) {
    return request({
      url: `/priceRule/getPriceRule/${id}`,
      method: 'GET'
    })
  }
}

export default api