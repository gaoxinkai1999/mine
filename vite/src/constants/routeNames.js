/**
 * 路由名称常量
 * 命名规则：模块名_页面名
 * 例如：SHOP_CREATE 表示商店模块的创建页面
 */
export const ROUTE_NAMES = {
  // 主页
  HOME: 'home',
  
  // 工具页面
  TOOLS: 'tools',
  
  // 我的页面
  MINE: 'mine',
  
  // 商店模块
  SHOP_LIST: 'shop-list',
  SHOP_CREATE: 'shop-create',
  SHOP_DETAIL: 'shop-detail',
  SHOP_ARREARS: 'shop-arrears',
  SHOP_MAP: 'shop-map',
  
  // 订单模块
  ORDER_HOME: 'order-home',
  ORDER_SALE_LIST: 'order-sale-list',
  ORDER_SALE_NEW: 'order-sale-new',
  ORDER_RETURN_NEW: 'order-return-new',
  ORDER_LIMIT: 'order-limit',
  // 产品模块
  PRODUCT_LIST: 'product-list',
  PRODUCT_DETAIL: 'product-detail',
  PRODUCT_INVENTORY: 'product-inventory',
  PRODUCT_ORDER_LIST: 'product-order-list',
  MONITOR_EXPIRY : 'expiry-monitor', // 监控过期
  
  // 采购模块
  PURCHASE_LIST: 'purchase-list',
  PURCHASE_NEW: 'purchase-new',
  
  // 统计模块
  STATISTICS_HOME: 'statistics-home',
  STATISTICS_DAILY: 'statistics-daily',
  STATISTICS_MONTHLY: 'statistics-monthly',
  STATISTICS_CHART: 'statistics-chart',
  STATISTICS_SHOP: 'statistics-shop',
  
  // 其他
  DEMO: 'demo',

  // 设置模块
  SETTINGS_PRINT: 'print-settings' // 对应 src/pages/PrintSettings.vue
}