import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'
import { showToast, showSuccessToast, showFailToast } from 'vant'

/**
 * 采购管理Store
 * 负责管理采购订单、商品列表等相关状态和业务逻辑
 */
export const usePurchaseStore = defineStore('purchase', () => {
    // ==================== 状态定义 ====================
    /** 商品列表 */
    const products = ref([])
    /** 品类列表*/
    const categories = ref([])
    
    /** 采购清单 */
    const purchaseList = ref([])
    
    // ==================== 计算属性 ====================
    /**
     * 采购总金额
     */
    const totalAmount = computed(() =>
        purchaseList.value.reduce((total, item) => total + item.totalAmount, 0)
    )

    // ==================== 方法定义 ====================
    /**
     * 初始化数据
     */
    async function init() {
        await getCategories()
        await getProducts()

    }

    /**
     * 获取商品列表
     */
    async function getProducts() {
        try {
            products.value = await api.purchase.getOnSaleProductsWithPurchaseInfo()
        } catch (error) {
            console.error('获取商品列表失败:', error)
            showFailToast('获取商品列表失败')
        }
    }

    /**
     * 获取品类列表
     */
    async function getCategories() {
        try {
            categories.value = await api.category.getCategories()
        } catch (error) {
            console.error('获取商品列表失败:', error)
            showFailToast('获取商品列表失败')
        }
    }

    /**
     * 更新采购清单
     * @param {Object} item - 采购项信息
     */
    function updatePurchaseList(item) {
        const index = purchaseList.value.findIndex(i => i.productId === item.productId)
        if (item.quantity === 0) {
            if (index > -1) {
                purchaseList.value.splice(index, 1)
            }
        } else if (index > -1) {
            purchaseList.value[index] = { ...item }
        } else {
            purchaseList.value.push({ ...item })
        }
    }

    /**
     * 清空采购清单
     */
    function clearPurchaseList() {
        purchaseList.value = []
    }

    /**
     * 提交采购订单
     */
    async function submitPurchase() {
        if (purchaseList.value.length === 0) {
            showToast('请先添加采购商品')
            return false
        }

        try {
            const request = {
                details: purchaseList.value.map(item => ({
                    productId: item.productId,
                    quantity: item.quantity,
                    totalAmount: item.totalAmount,
                    productionDate: item.productionDate,
                    expirationDate: item.expirationDate
                }))
            }

            await api.purchase.createPurchase(request)
            showSuccessToast('提交成功')
            clearPurchaseList()
            return true
        } catch (error) {
            console.error('提交采购单失败:', error)
            showFailToast('提交失败，请重试')
            return false
        }
    }

    return {
        // 状态
        products,
        purchaseList,
        categories,
        // 计算属性
        totalAmount,
        // 方法
        init,
        updatePurchaseList,
        clearPurchaseList,
        submitPurchase
    }
})