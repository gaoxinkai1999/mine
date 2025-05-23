import {defineStore} from 'pinia'
import {ref, computed} from 'vue'
import api from '@/api'
import locationService from '@/utils/locationService'
import {showToast, showSuccessToast, showFailToast} from 'vant'

/**
 * 订单管理Store
 * 负责管理商品订单、购物车、商品分类、商品列表等相关状态和业务逻辑
 */
export const useOrderStore = defineStore('order', () => {
    // ==================== 状态定义 ====================
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
        cart.value.reduce((total, item) => total + item.price * item.count, 0)
    )

    /**
     * 购物车商品总数量
     * 计算购物车中所有商品的数量之和
     */
    const totalCount = computed(() =>
        cart.value.reduce((sum, item) => sum + item.count, 0)
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
            const products = await api.product.getProducts()
            foods.value = products.map(food => ({
                ...food,
                price: food.defaultSalePrice,
                count: 0
            }))
        } catch (error) {
            console.error('获取商品失败:', error)
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
        // 此方法仅处理非批次管理的商品更新
        if (item.batchManaged) {
            console.warn('updateCart should not be called for batch managed items. Use updateCartWithBatch instead.');
            return;
        }

        const cartItem = cart.value.find(i => i.id === item.id);
        if (item.count === 0) {
            // 从购物车移除
            cart.value = cart.value.filter(i => i.id !== item.id);
        } else if (cartItem) {
            // 更新购物车中现有商品的数量
            cartItem.count = item.count;
            // 如果价格有变动也需要更新，这里假设价格在添加时确定或通过其他机制修改
            cartItem.price = item.price; // 更新价格以支持修改价格功能
        } else {
            // 添加新商品到购物车 (非批次管理)
            cart.value.push({
                id: item.id,
                name: item.name,
                price: item.price, // 使用传入的 price，可能已被修改
                count: item.count,
                batchManaged: false, // 明确非批次管理
                batchDetails: null, // 非批次管理无批次详情
                defaultSalePrice: item.defaultSalePrice // 保留原始售价
            });
        }

        // 同步更新 foods 列表中的数量 (无论是否在购物车中)
        const foodItem = foods.value.find(i => i.id === item.id);
        if (foodItem) {
            foodItem.count = item.count;
            // 如果价格被修改，也同步更新 foods 中的 price
            foodItem.price = item.price;
        }
    }

    /**
     * 更新购物车中的批次管理商品
     * @param {Object} payload - 包含商品ID和批次详情 { productId, batchDetails }
     * batchDetails: [{ batchId, batchNumber, quantity }]
     */
    function updateCartWithBatch(payload) {
        const { productId, batchDetails } = payload;
        const foodItem = foods.value.find(i => i.id === productId);
        if (!foodItem) {
            console.error(`Product with ID ${productId} not found in foods list.`);
            return;
        }

        // 计算总数量
        const totalQuantity = batchDetails.reduce((sum, detail) => sum + detail.quantity, 0);

        // 更新 foods 列表中的总数量
        foodItem.count = totalQuantity;

        // 更新购物车
        const cartItemIndex = cart.value.findIndex(i => i.id === productId);

        if (totalQuantity > 0) {
            const newBatchDetails = batchDetails.filter(detail => detail.quantity > 0); // 只保留数量大于0的批次

            if (cartItemIndex > -1) {
                // 更新购物车中现有商品
                cart.value[cartItemIndex].count = totalQuantity;
                cart.value[cartItemIndex].batchDetails = newBatchDetails;
                // 批次商品的价格通常在选择批次时不修改，以商品默认售价为准或通过特定修改价格入口
                // cart.value[cartItemIndex].price = foodItem.price;
            } else {
                // 添加新商品到购物车 (批次管理)
                cart.value.push({
                    id: foodItem.id,
                    name: foodItem.name,
                    price: foodItem.price, // 使用 foods 中的当前价格 (通常是 defaultSalePrice)
                    count: totalQuantity,
                    batchManaged: true, // 明确批次管理
                    batchDetails: newBatchDetails,
                    defaultSalePrice: foodItem.defaultSalePrice // 保留原始售价
                });
            }
        } else {
            // 如果总数量为0，从购物车移除
            if (cartItemIndex > -1) {
                cart.value.splice(cartItemIndex, 1);
            }
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

    /**
     * 提交订单
     * @returns {Promise<boolean>} 下单是否成功
     * 1. 检查购物车是否为空
     * 2. 构建订单数据并提交
     * 3. 下单成功后清空购物车
     */
    async function submitOrder() {
        if (cart.value.length === 0) {
            showToast('请先选择商品')
            return false
        }

        try {
            const order = {
                shopId: currentShop.value.id,
                items: cart.value.map(item => {
                    const orderItem = {
                        productId: item.id,
                        quantity: item.count,
                        price: item.price // 使用购物车中的当前价格（可能已被修改）
                    };
                    // 如果是批次管理的商品且有批次详情，则添加到请求体
                    if (item.batchManaged && item.batchDetails && item.batchDetails.length > 0) {
                        // 过滤掉数量为0的批次详情，以防万一
                        const validBatchDetails = item.batchDetails.filter(bd => bd.quantity > 0);
                        if (validBatchDetails.length > 0) {
                           orderItem.batchDetails = validBatchDetails.map(bd => ({
                                batchId: bd.batchId,
                                quantity: bd.quantity
                                // 后端 OrderService.java 确认只需要 batchId 和 quantity
                            }));
                        }
                    }
                    return orderItem;
                }),
            };

            console.log("Submitting order:", JSON.stringify(order, null, 2)); // 增加日志方便调试

            await api.order.createOrder(order);
            showSuccessToast('下单成功')
            clearCart()
            return true
        } catch (error) {
            console.error('下单失败:', error)
            showFailToast('下单失败，请重试')
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
        updateCart, // 处理非批次商品
        updateCartWithBatch, // 处理批次商品
        clearCart,
        submitOrder
    }
})