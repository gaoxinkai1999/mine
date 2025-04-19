import request from '@/utils/axiosConfig'

const api = {
  /**
   * @description 更新店铺信息
   * @url /shop/update
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  update(data) {
    return request({
      url: '/shop/update',
      method: 'POST',
      data
    })
  },

  /**
   * @description 新建店铺
   * @url /shop/create
   * @method POST
   * @param {Object} data Request body
   * @returns {Promise<any>}
   */
  create(data) {
    return request({
      url: '/shop/create',
      method: 'POST',
      data
    })
  },

  /**
   * @description 模糊查询商家
   * @url /shop/search
   * @method GET
   * @param {Object} params Query parameters
   * @param {string} params.name 
   * @returns {Promise<any>}
   */
  searchShops(params = {}) {
    return request({
      url: '/shop/search',
      method: 'GET',
      params
    })
  },

  /**
   * @description 获取店铺位置信息列表
   * @url /shop/locations
   * @method GET

   * @returns {Promise<any>}
   */
  getShopLocations() {
    return request({
      url: '/shop/locations',
      method: 'GET'
    })
  },

  /**
   * @description 获取店铺列表
   * @url /shop/list
   * @method GET

   * @returns {Promise<any>}
   */
  getShops() {
    return request({
      url: '/shop/list',
      method: 'GET'
    })
  },

  /**
   * @description 
   * @url /shop/inactive-shops
   * @method GET
   * @param {Object} params Query parameters
   * @param {number} params.days 
   * @returns {Promise<any>}
   */
  getInactiveShops(params = {}) {
    return request({
      url: '/shop/inactive-shops',
      method: 'GET',
      params
    })
  },

  /**
   * @description 按拼音分组门店
   * @url /shop/group-by-pinyin
   * @method GET

   * @returns {Promise<any>}
   */
  groupShopsByPinyin() {
    return request({
      url: '/shop/group-by-pinyin',
      method: 'GET'
    })
  },

  /**
   * @description 获取单个店铺
   * @url /shop/get
   * @method GET
   * @param {Object} params Query parameters
   * @param {number} params.id 
   * @returns {Promise<any>}
   */
  getShop(params = {}) {
    return request({
      url: '/shop/get',
      method: 'GET',
      params
    })
  },

  /**
   * @description 获取商家欠款数据
   * @url /shop/arrears
   * @method GET

   * @returns {Promise<any>}
   */
  arrears() {
    return request({
      url: '/shop/arrears',
      method: 'GET'
    })
  },

  /**
   * @description 
   * @url /shop/active-shops
   * @method GET
   * @param {Object} params Query parameters
   * @param {number} params.days 
   * @returns {Promise<any>}
   */
  getActiveShops(params = {}) {
    return request({
      url: '/shop/active-shops',
      method: 'GET',
      params
    })
  }
}

export default api