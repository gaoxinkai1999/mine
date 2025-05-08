/**
 * 路由名称常量
 * 命名规则：模块名_页面名
 * 例如：SHOP_CREATE 表示商店模块的创建页面
 */
export const ROUTE_NAMES = {
  // 主页
  HOME: 'index', // 对应 pages/index.vue
  
  // 工具页面
  TOOLS: 'tools',
  
  // 我的页面
  MINE: 'mine',
  
  // 商店模块
  SHOP_LIST: 'shop',
  SHOP_CREATE: 'shop-new', // 对应 pages/shop/new.vue
  SHOP_DETAIL: 'shop-info', // 对应 pages/shop/info.vue
  SHOP_ARREARS: 'shop-arrears',
  SHOP_MAP: 'map', // 对应 pages/map/index.vue
  
  // 订单模块
  ORDER_HOME: 'order', // 对应 pages/order/index.vue
  ORDER_SALE_LIST: 'order-sale', // 对应 pages/order/sale/index.vue
  ORDER_SALE_NEW: 'order-sale-new',
  ORDER_RETURN_NEW: 'order-return-new',
  ORDER_LIMIT: 'order-limit',
  // 产品模块
  PRODUCT_LIST: 'product', // 对应 pages/product/index.vue
  PRODUCT_DETAIL: 'product-detail',
  PRODUCT_INVENTORY: 'product-inventory',
  PRODUCT_ORDER_LIST: 'product-order-list', // 对应 pages/product/order-list.vue
  MONITOR_EXPIRY : 'monitor-expiry', // 对应 pages/monitor/expiry.vue
  MONITOR_PROCESSED_HISTORY: 'monitor-processed-history', // 对应 pages/monitor/processed-history.vue
  
  // 采购模块
  PURCHASE_LIST: 'purchase', // 对应 pages/purchase/index.vue
  PURCHASE_NEW: 'purchase-new',
  
  // 统计模块
  STATISTICS_HOME: 'statistics', // 对应 pages/statistics/index.vue
  STATISTICS_DAILY: 'statistics-dailyData',
  STATISTICS_MONTHLY: 'statistics-monthData',
  STATISTICS_CHART: 'statistics-chart',
  STATISTICS_SHOP: 'statistics-shop',
  
  // 其他
  DEMO: 'demo',

  // 设置模块
  SETTINGS_PRINT: 'print-settings' // 对应 src/pages/PrintSettings.vue
}