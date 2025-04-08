<route>
{
name: "statistics-monthly"
}
</route>

<template>
  <van-pull-refresh v-model="loading" @refresh="onRefresh">
    <div class="month-statistics">
      <div class="page-header">
        <h2 class="title">月度销售数据</h2>
        <!-- 显示实际获取到的数据范围或固定范围 -->
        <span class="date-range">{{ startDateDisplay }} 至 {{ endDateDisplay }}</span>
      </div>

      <!-- 数据卡片组 -->
      <div class="stats-cards">
        <van-empty v-if="!tableData || tableData.length === 0 && !loading" description="暂无数据" />

        <!-- 修改 v-for 循环，直接遍历处理后的 tableData 数组 -->
        <StatsCard
            v-for="(data, index) in tableData"
            :key="data.yearMonth || index"
        :title="data.monthName"
        :tag="(data.growthRate > 0 ? '+' : '') + (data.growthRate * 100).toFixed(2) + '%'"
        :tag-class="getGrowthClass(data.growthRate)"
        :stats="data"
        :expanded="expandedCard === (data.yearMonth || index)"
        @toggle-expand="toggleCard(data.yearMonth || index)"
        />
      </div>
    </div>
  </van-pull-refresh>
</template>

<script>
import api from "@/api/index.js";
import { showToast } from "vant";
import StatsCard from "@/components/StatsCard.vue";
import { ref, computed, onMounted } from 'vue'; // 引入 Vue 3 Composition API

export default {
  name: "MonthData",
  components: {
    StatsCard
  },
  setup() { // 使用 setup 函数
    const tableData = ref([]); // 月度统计数据数组
    const expandedCard = ref(null); // 当前展开的卡片 key
    const loading = ref(false); // 下拉刷新状态
    const initialStartDate = "2024-02-01"; // 初始开始日期
    const startDate = ref(initialStartDate); // 实际使用的开始日期 (可以响应式)
    const endDate = ref(new Date()); // 结束日期默认为今天

    // 显示用的日期范围
    const startDateDisplay = computed(() => formatDate(new Date(startDate.value)));
    const endDateDisplay = computed(() => formatDate(endDate.value));

    // --- 方法 ---
    const formatDate = (date) => {
      if (!date || isNaN(new Date(date))) return '';
      const d = new Date(date);
      return d.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      }).replace(/\//g, '-');
    };

    const getGrowthClass = (growth) => {
      if (growth > 0) return 'growth-positive';
      if (growth < 0) return 'growth-negative';
      return 'growth-neutral';
    };

    const toggleCard = (key) => {
      expandedCard.value = expandedCard.value === key ? null : key;
    };

    const onRefresh = async () => {
      loading.value = true;
      await getData();
      // 移除 setTimeout，让加载状态在数据获取后自然结束
      showToast("刷新成功");
      loading.value = false;
    };

    // 格式化后端返回的 YearMonth 字符串为 "YYYY年MM月"
    const formatMonthName = (yearMonthStr) => {
      if (!yearMonthStr || typeof yearMonthStr !== 'string') return '未知月份';
      try {
        const [year, month] = yearMonthStr.split('-');
        return `${year}年${parseInt(month, 10)}月`;
      } catch (e) {
        return '格式错误';
      }
    };

    // 计算环比增长率 (在前端计算)
    const calculateGrowthRate = (currentProfit, previousProfit) => {
      if (previousProfit && previousProfit !== 0) {
        return (currentProfit - previousProfit) / previousProfit;
      }
      return 0; // 如果上月利润为0或不存在，增长率为0
    };

    // 修改后的 getData 方法
    const getData = async () => {
      loading.value = true; // 开始加载状态
      try {
        const formattedStartDate = formatDate(new Date(startDate.value));
        const formattedEndDate = formatDate(endDate.value);

        // **一次性调用新的后端接口**
        const responseData = await api.statistics.getMonthlyStatistics({
          startDate: formattedStartDate,
          endDate: formattedEndDate
        });

        // **处理后端返回的数据 (假设后端返回 Map<String, SalesStatisticsDTO>，key为"YYYY-MM")**
        const processedData = [];
        let previousProfit = null; // 用于计算环比

        // 将 Map 转换为有序数组 (假设后端返回的Map是按时间顺序的，比如LinkedHashMap)
        // 如果后端返回的是无序Map，需要先排序
        const sortedKeys = Object.keys(responseData).sort(); // 按 "YYYY-MM" 排序

        for (const yearMonthStr of sortedKeys) {
          const monthStats = responseData[yearMonthStr];
          const growthRate = calculateGrowthRate(monthStats.totalProfit, previousProfit);

          processedData.push({
            ...monthStats,
            yearMonth: yearMonthStr, // 保存 YYYY-MM 字符串作为 key
            monthName: formatMonthName(yearMonthStr),
            growthRate: growthRate
          });
          previousProfit = monthStats.totalProfit; // 更新上个月利润
        }

        // **倒序排列，让最新的月份显示在最前面**
        tableData.value = processedData.reverse();

      } catch (error) {
        showToast("获取数据失败");
        console.error('获取月度统计数据失败:', error);
      } finally {
        loading.value = false; // 结束加载状态
      }
    };

    // 生命周期钩子
    onMounted(() => {
      getData();
    });

    return {
      tableData,
      expandedCard,
      loading,
      startDateDisplay,
      endDateDisplay,
      formatDate,
      getGrowthClass,
      toggleCard,
      onRefresh,
      getData
    };
  }
}
</script>

<style scoped>
.month-statistics {
  padding: 16px;
  background-color: #f7f8fa;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 16px;
  text-align: center;
}

.title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 8px;
  color: #323233;
}

.date-range {
  font-size: 14px;
  color: #969799;
}

.stats-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* StatsCard 的样式在组件内部定义，这里无需重复 */

/* 可以在这里添加特定于 MonthData 页面的额外样式 */
</style>