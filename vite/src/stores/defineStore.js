import {defineStore} from 'pinia'

// 使用 defineStore 定义 store
export const useMyStore = defineStore('my', {
    // 状态
    state: () => ({
        version: 'xxx',
        changelog: 'xxx',



    }),

    // 计算属性
    getters: {},

    // 方法
    actions: {

    },
    persist: true  // 开启持久化

})
