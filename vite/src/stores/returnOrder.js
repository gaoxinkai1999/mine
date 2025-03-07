import {defineStore} from 'pinia'
import {ref, computed} from 'vue'
import api from '@/api'
import locationService from '@/utils/locationService'
import {showToast, showSuccessToast, showFailToast} from 'vant'
import { useRouter } from 'vue-router'

/**
 * 订单管理Store
 * 负责管理商品订单、购物车、商品分类、商品列表等相关状态和业务逻辑
 */
export const useReturnOrderStore = defineStore('returnOrder', () => {
    // ==================== 状态定义 ====================
    const router = useRouter()
    /** 当前定位信息 */
    const currentLocation = ref({})

    /** 当前选中的分类索引 */
    const activeCategory = ref(0)

    /** 商品分类列表 */
    const categories = ref([])

    /** 商品列表 */
    const foods = ref([])

    /** 购物车列表 */
    const cart = ref([])

    /** 附近商铺列表 */
    const nearbyShops = ref([{
        "id": 128,
        "name": "育青苑小卖部",
        "latitude": 37.602938,
        "longitude": 112.348135,
        "address": "育青苑小区",
        "distance": 43.45429229736328
    }, {
        "id": 243,
        "name": "晋乐山驴肉店",
        "latitude": 37.60148,
        "longitude": 112.350471,
        "address": "商业南街",
        "distance": 270.90704345703125
    }])

    /** 是否显示购物车面板 */
    const showCart = ref(false)

    /** 当前选中的商铺信息 */
    const currentShop = ref({})

    // ==================== 计算属性 ====================
    /**
     * 当前分类下的商品列表
     * 根据选中的分类ID过滤商品
     */
    const currentFoods = computed(() =>
        foods.value.filter(food =>
            food.categoryId == categories.value[activeCategory.value]?.id
        )
    )

    /**
     * 购物车总价
     * 计算购物车中所有商品的总金额
     */
    const totalPrice = computed(() =>
        cart.value.reduce((total, item) => total + Number(item.amount), 0)
    )

    /**
     * 购物车商品总数量
     * 计算购物车中所有商品的数量之和
     */
    const totalCount = computed(() =>
        cart.value.reduce((sum, item) => sum + 1, 0)
    )

    // ==================== 方法定义 ====================
    /**
     * 初始化数据
     * 获取附近商铺、商品分类和商品列表
     */
    async function init() {
        await getNearbyShops()
        await Promise.all([
            getCategories(),
            getProducts()
        ]);
    }

    /**
     * 获取附近商铺信息
     * 1. 获取当前位置和附近商铺列表
     * 2. 获取默认选中商铺的详细信息
     */
    async function getNearbyShops() {
        try {
            const result = await locationService.getNearbyShops();
            currentLocation.value = result.currentLocation;
            nearbyShops.value = result.nearbyShops;

        } catch (error) {
            console.error('获取位置信息失败:', error);
        } finally {
            const shop = await api.shop.getShop({id: nearbyShops.value[0].id});
            currentShop.value = {
                ...shop,
                distance: nearbyShops.value[0].distance
            }
        }
    }

    /**
     * 获取商品分类列表
     */
    async function getCategories() {
        try {
            categories.value = await api.category.getCategories()
        } catch (error) {
            console.error('获取分类失败:', error)
        }
    }

    /**
     * 获取商品列表
     * 获取当前选中商铺的所有商品，并初始化商品数量为0
     */
    async function getProducts() {
        try {
            const products = await api.product.getProductSaleList({shopId: currentShop.value.id})
            foods.value = products.map(food => ({
                ...food,
                count: 0
            }))
        } catch (error) {
            console.error('获取商品失败:', error)
        }
    }

    /**
     * 切换当前商家
     * @param {Object} shop - 新选择的商家信息
     * 1. 更新当前商家
     * 2. 重新获取该商家的商品和分类
     * 3. 清空购物车
     */
    async function changeShop(shop) {
        try {
            // 获取完整的商家信息
            const fullShopInfo = await api.shop.getShop({id: shop.id});
            
            // 更新当前商家
            currentShop.value = {
                ...fullShopInfo,
                distance: shop.distance
            }
            
            // 清空购物车
            clearCart()
            
            // 重新获取分类和商品
            await Promise.all([
                getCategories(),
                getProducts()
            ]);
            
            // 重置分类索引
            activeCategory.value = 0;
            
            return true;
        } catch (error) {
            console.error('切换商家失败:', error)
            showFailToast('切换商家失败，请重试')
            return false
        }
    }

    /**
     * 更新购物车
     * @param {Object} item - 要更新的商品信息
     * 1. 如果商品数量为0，从购物车中移除
     * 2. 如果购物车中已存在，更新数量
     * 3. 如果是新商品，添加到购物车（只保存必要属性）
     * 4. 同步更新商品列表中的数量
     */
    function updateCart(item) {
        const cartItem = cart.value.find(i => i.id === item.id)
        if (item.count === 0) {
            cart.value = cart.value.filter(i => i.id !== item.id)
        } else if (cartItem) {
            cartItem.count = item.count
        } else {
            // 只保存购物车所需的必要属性
            cart.value.push({
                id: item.id,
                name: item.name,
                price: item.price,
                count: item.count,
            })
        }

        const foodItem = foods.value.find(i => i.id === item.id)
        if (foodItem) {
            foodItem.count = item.count
        }
    }

    /**
     * 清空购物车
     * 清除购物车内容并重置所有商品数量为0
     */
    function clearCart() {
        cart.value = []
        foods.value.forEach(item => item.count = 0)
    }

    function removeCartItem(item) {
        const index= cart.value.indexOf(item)
        if (index !== -1) {
            cart.value.splice(index, 1);
        }
    }

    /**
     * 提交退货订单
     * @returns {Promise<boolean>} 提交退货订单是否成功
     * 1. 检查购物车是否为空
     * 2. 构建退货订单数据并提交
     * 3. 提交成功后清空购物车
     */
    async function submitReturnOrder() {
        if (cart.value.length === 0) {
            showToast('请先选择退货商品')
            return false
        }

        try {
            const returnOrder = {
                shopId: currentShop.value.id,
                details: cart.value.map(item => ({
                    productId: item.id,
                    amount: Number(item.amount),
                    type: item.type, // 退货类型已经是字符串，后端会自动转成枚举
                    quantity: item.type === '退货退款' ? Number(item.quantity) : null // 退货数量，仅退款类型为null
                })),
            }

            await api.returnorder.createReturnOrder(returnOrder)
            showSuccessToast('退货单提交成功')
            clearCart()
            // 返回订单列表页
            router.push('/order/list')
            return true
        } catch (error) {
            console.error('退货单提交失败:', error)
            showFailToast('退货单提交失败，请重试')
            return false
        }
    }

    // 导出状态和方法
    return {
        // 状态
        activeCategory,
        categories,
        foods,
        cart,
        nearbyShops,
        showCart,
        currentLocation,
        currentShop,
        // 计算属性
        currentFoods,
        totalPrice,
        totalCount,
        // 方法
        init,
        updateCart,
        clearCart,
        submitReturnOrder,
        removeCartItem,
        changeShop
    }
})