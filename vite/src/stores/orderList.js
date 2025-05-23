import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { showSuccessToast, showFailToast } from 'vant'
import { Clipboard } from '@capacitor/clipboard'
import { formatMergedReceipt, printMergedOrders } from "@/utils/printService"

/**
 * 订单列表管理Store
 * 负责管理订单列表的筛选参数、多选状态等
 */
export const useOrderListStore = defineStore('orderList', () => {
  // ==================== 状态定义 ====================
  /** 筛选参数 */
  const filterParams = ref({
    shopId: null,
    shopName: '',
    startDate: null,
    endDate: null
  })
  
  /** 是否处于多选模式 */
  const isSelectionMode = ref(false)
  
  /** 已选择的订单列表 */
  const selectedOrders = ref([])
  
  /** 活动tab索引 */
  const activeTab = ref('0')
  
  // ==================== 计算属性 ====================
  /** 是否有筛选条件 */
  const hasFilters = computed(() => {
    return filterParams.value.shopId || filterParams.value.startDate || filterParams.value.endDate
  })
  
  /** 日期范围文本显示 */
  const dateRangeText = computed(() => {
    if (filterParams.value.startDate && filterParams.value.endDate) {
      return `${filterParams.value.startDate} 至 ${filterParams.value.endDate}`
    } else if (filterParams.value.startDate) {
      return `从 ${filterParams.value.startDate} 开始`
    } else if (filterParams.value.endDate) {
      return `至 ${filterParams.value.endDate}`
    }
    return ''
  })
  
  /** 所有选中的订单是否来自同一个商店 */
  const isSameShop = computed(() => {
    if (selectedOrders.value.length <= 1) return true
    
    const firstShopId = selectedOrders.value[0].shop.id
    return selectedOrders.value.every(order => order.shop.id === firstShopId)
  })
  
  // ==================== 方法定义 ====================
  /**
   * 设置筛选参数
   * @param {Object} params - 筛选参数对象
   */
  function setFilterParams(params) {
    if (params.shopId !== undefined) filterParams.value.shopId = params.shopId
    if (params.shopName !== undefined) filterParams.value.shopName = params.shopName
    if (params.startDate !== undefined) filterParams.value.startDate = params.startDate
    if (params.endDate !== undefined) filterParams.value.endDate = params.endDate
  }
  
  /**
   * 清除所有筛选
   */
  function clearAllFilters() {
    // 完全重置筛选参数对象，确保状态更新
    filterParams.value = {
      shopId: null,
      shopName: '',
      startDate: null,
      endDate: null
    }
  }
  
  /**
   * 开始多选模式
   */
  function startSelection() {
    isSelectionMode.value = true
    selectedOrders.value = []
  }
  
  /**
   * 取消多选模式
   */
  function cancelSelection() {
    isSelectionMode.value = false
    selectedOrders.value = []
  }
  
  /**
   * 处理订单选中事件
   * @param {Object} order - 订单对象
   * @param {Boolean} isSelected - 是否选中
   */
  function handleOrderSelected(order, isSelected) {
    if (isSelected) {
      selectedOrders.value.push(order)
    } else {
      selectedOrders.value = selectedOrders.value.filter(item => item.id !== order.id)
    }
  }
  
  /**
   * 合并打印订单
   */
  async function handleMergedPrint() {
    if (selectedOrders.value.length === 0) {
      showFailToast('请先选择订单')
      return
    }
    
    if (!isSameShop.value) {
      showFailToast('只能合并来自同一商店的订单')
      return
    }
    
    try {
      await printMergedOrders(selectedOrders.value, (status) => {
        console.log(status)
      })
      cancelSelection() // 打印成功后退出选择模式
    } catch (error) {
      console.error('合并打印失败', error)
    }
  }
  
  /**
   * 合并复制订单
   */
  async function handleMergedCopy() {
    if (selectedOrders.value.length === 0) {
      showFailToast('请先选择订单')
      return
    }
    
    if (!isSameShop.value) {
      showFailToast('只能合并来自同一商店的订单')
      return
    }
    
    try {
      const receipt = formatMergedReceipt(selectedOrders.value, false)
      await Clipboard.write({
        string: receipt
      })
      showSuccessToast('合并订单已复制到剪切板')
      cancelSelection() // 复制成功后退出选择模式
    } catch (err) {
      console.error('复制失败:', err)
      showFailToast('复制失败，错误信息:' + err)
    }
  }
  
  // 返回store的状态和方法
  return {
    // 状态
    filterParams,
    isSelectionMode,
    selectedOrders,
    activeTab,
    
    // 计算属性
    hasFilters,
    dateRangeText,
    isSameShop,
    
    // 方法
    setFilterParams,
    clearAllFilters,
    startSelection,
    cancelSelection,
    handleOrderSelected,
    handleMergedPrint,
    handleMergedCopy
  }
}) 