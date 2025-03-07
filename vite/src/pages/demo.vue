<template>
  <div class="bg-white p-4 rounded-lg shadow-lg">
    <h2 class="text-xl font-bold mb-4 text-center">Prophet销量预测可视化 (2025-2026)</h2>

    <div class="mb-4 flex justify-center">
      <div class="inline-flex rounded-md shadow-sm">
        <button
            class="px-4 py-2 text-sm font-medium rounded-l-lg"
            :class="activeTab === 'weekly' ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 border border-gray-300'"
            @click="activeTab = 'weekly'"
        >
          周度预测
        </button>
        <button
            class="px-4 py-2 text-sm font-medium"
            :class="activeTab === 'monthly' ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 border border-gray-300'"
            @click="activeTab = 'monthly'"
        >
          月度平均
        </button>
        <button
            class="px-4 py-2 text-sm font-medium rounded-r-lg"
            :class="activeTab === 'interval' ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 border border-gray-300'"
            @click="activeTab = 'interval'"
        >
          置信区间分析
        </button>
      </div>
    </div>

    <div v-if="activeTab === 'weekly'" class="mb-4">
      <div style="width: 100%; height: 400px;">
        <v-chart :option="weeklyChartOption" autoresize />
      </div>
    </div>

    <div v-if="activeTab === 'monthly'" class="mb-4">
      <div style="width: 100%; height: 400px;">
        <v-chart :option="monthlyChartOption" autoresize />
      </div>
    </div>

    <div v-if="activeTab === 'interval'" class="mb-4">
      <div style="width: 100%; height: 400px;">
        <v-chart :option="intervalChartOption" autoresize />
      </div>
    </div>

    <div class="mt-6 grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="bg-blue-50 p-4 rounded-lg border border-blue-200">
        <h3 class="font-bold text-blue-800">峰值预测</h3>
        <p class="mt-2">最高销量预计在 <span class="font-semibold">{{ maxValue.date }}</span>，达到 <span class="font-semibold">{{ maxValue.value.toFixed(1) }}</span> 单位</p>
      </div>

      <div class="bg-red-50 p-4 rounded-lg border border-red-200">
        <h3 class="font-bold text-red-800">低谷预测</h3>
        <p class="mt-2">最低销量预计在 <span class="font-semibold">{{ minValue.date }}</span>，仅为 <span class="font-semibold">{{ minValue.value.toFixed(1) }}</span> 单位</p>
      </div>

      <div class="bg-green-50 p-4 rounded-lg border border-green-200">
        <h3 class="font-bold text-green-800">季节性分析</h3>
        <p class="mt-2">明显的双峰模式：<span class="font-semibold">夏季(7-8月)</span> 和 <span class="font-semibold">春季(4-6月)</span> 为销售旺季</p>
      </div>
    </div>

    <div class="mt-6 bg-yellow-50 p-4 rounded-lg border border-yellow-200">
      <h3 class="font-bold text-yellow-800">使用指南</h3>
      <ul class="mt-2 space-y-1 list-disc pl-5">
        <li><span class="font-semibold">yhat</span>: 主要预测值，表示模型对未来销量的最佳估计</li>
        <li><span class="font-semibold">yhat_lower</span>: 预测下限，适用于保守规划场景</li>
        <li><span class="font-semibold">yhat_upper</span>: 预测上限，适用于确保资源充足的场景</li>
        <li>点击上方选项卡可切换不同视图</li>
      </ul>
    </div>
  </div>
</template>

<script>
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart } from 'echarts/charts';
import {
  GridComponent,
  TooltipComponent,
  TitleComponent,
  LegendComponent,
  ToolboxComponent,
  DataZoomComponent,
} from 'echarts/components';
import VChart from 'vue-echarts';

use([
  CanvasRenderer,
  LineChart,
  GridComponent,
  TooltipComponent,
  TitleComponent,
  LegendComponent,
  ToolboxComponent,
  DataZoomComponent,
]);

export default {
  name: 'SalesForecastVisualization',
  components: {
    VChart,
  },
  data() {
    return {
      activeTab: 'weekly',
      data: [
        { ds: "2025-03-02", yhat: 9.0, yhat_lower: 1.2, yhat_upper: 17.1 },
        { ds: "2025-03-09", yhat: 9.8, yhat_lower: 1.7, yhat_upper: 18.0 },
        { ds: "2025-03-16", yhat: 10.1, yhat_lower: 2.1, yhat_upper: 18.4 },
        { ds: "2025-03-23", yhat: 11.3, yhat_lower: 3.3, yhat_upper: 19.9 },
        { ds: "2025-03-30", yhat: 13.0, yhat_lower: 5.1, yhat_upper: 21.0 },
        { ds: "2025-04-06", yhat: 14.1, yhat_lower: 5.9, yhat_upper: 22.6 },
        { ds: "2025-04-13", yhat: 13.9, yhat_lower: 6.0, yhat_upper: 22.3 },
        { ds: "2025-04-20", yhat: 13.7, yhat_lower: 5.9, yhat_upper: 22.1 },
        { ds: "2025-04-27", yhat: 15.1, yhat_lower: 7.3, yhat_upper: 23.3 },
        { ds: "2025-05-04", yhat: 18.0, yhat_lower: 10.1, yhat_upper: 26.2 },
        { ds: "2025-05-11", yhat: 20.3, yhat_lower: 12.1, yhat_upper: 28.1 },
        { ds: "2025-05-18", yhat: 20.2, yhat_lower: 12.4, yhat_upper: 28.6 },
        { ds: "2025-05-25", yhat: 19.0, yhat_lower: 10.3, yhat_upper: 26.3 },
        { ds: "2025-06-01", yhat: 19.0, yhat_lower: 10.5, yhat_upper: 27.6 },
        { ds: "2025-06-08", yhat: 21.0, yhat_lower: 13.3, yhat_upper: 29.0 },
        { ds: "2025-06-15", yhat: 22.2, yhat_lower: 14.2, yhat_upper: 30.6 },
        { ds: "2025-06-22", yhat: 20.0, yhat_lower: 12.0, yhat_upper: 28.1 },
        { ds: "2025-06-29", yhat: 15.2, yhat_lower: 7.1, yhat_upper: 23.3 },
        { ds: "2025-07-06", yhat: 12.3, yhat_lower: 4.1, yhat_upper: 20.4 },
        { ds: "2025-07-13", yhat: 14.7, yhat_lower: 6.9, yhat_upper: 22.8 },
        { ds: "2025-07-20", yhat: 21.1, yhat_lower: 13.1, yhat_upper: 29.5 },
        { ds: "2025-07-27", yhat: 26.1, yhat_lower: 18.0, yhat_upper: 34.8 },
        { ds: "2025-08-03", yhat: 25.6, yhat_lower: 17.9, yhat_upper: 33.7 },
        { ds: "2025-08-10", yhat: 20.9, yhat_lower: 13.6, yhat_upper: 29.0 },
        { ds: "2025-08-17", yhat: 16.4, yhat_lower: 7.9, yhat_upper: 24.0 },
        { ds: "2025-08-24", yhat: 15.3, yhat_lower: 7.7, yhat_upper: 23.3 },
        { ds: "2025-08-31", yhat: 16.5, yhat_lower: 8.7, yhat_upper: 24.2 },
        { ds: "2025-09-07", yhat: 17.3, yhat_lower: 9.3, yhat_upper: 25.5 },
        { ds: "2025-09-14", yhat: 16.4, yhat_lower: 8.8, yhat_upper: 24.7 },
        { ds: "2025-09-21", yhat: 15.2, yhat_lower: 6.9, yhat_upper: 22.9 },
        { ds: "2025-09-28", yhat: 15.2, yhat_lower: 6.2, yhat_upper: 23.7 },
        { ds: "2025-10-05", yhat: 16.1, yhat_lower: 7.9, yhat_upper: 24.0 },
        { ds: "2025-10-12", yhat: 16.4, yhat_lower: 8.3, yhat_upper: 24.7 },
        { ds: "2025-10-19", yhat: 15.2, yhat_lower: 7.3, yhat_upper: 22.6 },
        { ds: "2025-10-26", yhat: 13.8, yhat_lower: 5.8, yhat_upper: 21.7 },
        { ds: "2025-11-02", yhat: 13.6, yhat_lower: 5.0, yhat_upper: 21.6 },
        { ds: "2025-11-09", yhat: 14.4, yhat_lower: 6.4, yhat_upper: 22.7 },
        { ds: "2025-11-16", yhat: 15.2, yhat_lower: 7.5, yhat_upper: 23.5 },
        { ds: "2025-11-23", yhat: 15.1, yhat_lower: 7.2, yhat_upper: 23.0 },
        { ds: "2025-11-30", yhat: 14.4, yhat_lower: 5.5, yhat_upper: 22.5 },
        { ds: "2025-12-07", yhat: 13.6, yhat_lower: 5.7, yhat_upper: 22.2 },
        { ds: "2025-12-14", yhat: 12.5, yhat_lower: 4.7, yhat_upper: 20.6 },
        { ds: "2025-12-21", yhat: 10.6, yhat_lower: 2.6, yhat_upper: 18.5 },
        { ds: "2025-12-28", yhat: 8.6, yhat_lower: 0.6, yhat_upper: 16.1 },
        { ds: "2026-01-04", yhat: 8.3, yhat_lower: -0.1, yhat_upper: 16.8 },
        { ds: "2026-01-11", yhat: 10.1, yhat_lower: 1.6, yhat_upper: 18.0 },
        { ds: "2026-01-18", yhat: 12.3, yhat_lower: 4.5, yhat_upper: 20.8 },
        { ds: "2026-01-25", yhat: 12.3, yhat_lower: 4.2, yhat_upper: 20.1 },
        { ds: "2026-02-01", yhat: 9.7, yhat_lower: 1.7, yhat_upper: 18.0 },
        { ds: "2026-02-08", yhat: 6.9, yhat_lower: -1.4, yhat_upper: 14.6 },
        { ds: "2026-02-15", yhat: 6.7, yhat_lower: -1.6, yhat_upper: 15.0 },
        { ds: "2026-02-22", yhat: 9.2, yhat_lower: 0.7, yhat_upper: 17.4 },
        { ds: "2026-03-01", yhat: 12.2, yhat_lower: 4.5, yhat_upper: 20.4 },
        { ds: "2026-03-08", yhat: 13.6, yhat_lower: 5.6, yhat_upper: 21.6 },
        { ds: "2026-03-15", yhat: 14.0, yhat_lower: 5.5, yhat_upper: 21.6 },
        { ds: "2026-03-22", yhat: 15.3, yhat_lower: 7.8, yhat_upper: 23.3 },
        { ds: "2026-03-29", yhat: 17.6, yhat_lower: 9.2, yhat_upper: 25.9 },
        { ds: "2026-04-05", yhat: 19.4, yhat_lower: 10.8, yhat_upper: 27.0 },
        { ds: "2026-04-12", yhat: 19.3, yhat_lower: 11.0, yhat_upper: 27.1 },
        { ds: "2026-04-19", yhat: 18.8, yhat_lower: 10.6, yhat_upper: 26.6 }
      ],
    };
  },
  computed: {
    // 将日期格式化为月份
    formattedData() {
      return this.data.map(item => {
        const date = new Date(item.ds);
        return {
          ...item,
          month: `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}`
        };
      });
    },

    // 按月分组的数据
    monthlyAverages() {
      const monthlyData = {};

      this.formattedData.forEach(item => {
        if (!monthlyData[item.month]) {
          monthlyData[item.month] = {
            month: item.month,
            yhat: [],
            yhat_lower: [],
            yhat_upper: []
          };
        }
        monthlyData[item.month].yhat.push(item.yhat);
        monthlyData[item.month].yhat_lower.push(item.yhat_lower);
        monthlyData[item.month].yhat_upper.push(item.yhat_upper);
      });

      return Object.values(monthlyData).map(monthData => {
        const avg = arr => arr.reduce((sum, val) => sum + val, 0) / arr.length;
        return {
          month: monthData.month,
          yhat: avg(monthData.yhat),
          yhat_lower: avg(monthData.yhat_lower),
          yhat_upper: avg(monthData.yhat_upper)
        };
      });
    },

    // 计算预测区间宽度
    intervalData() {
      return this.data.map(item => ({
        ...item,
        interval_width: item.yhat_upper - item.yhat_lower
      }));
    },

    // 最高和最低预测值
    maxValue() {
      return this.data.reduce((max, item) =>
              item.yhat > max.value ? { value: item.yhat, date: item.ds } : max,
          { value: 0, date: '' }
      );
    },

    minValue() {
      return this.data.reduce((min, item) =>
              item.yhat < min.value || min.value === 0 ? { value: item.yhat, date: item.ds } : min,
          { value: 0, date: '' }
      );
    },

    // 周度图表配置
    weeklyChartOption() {
      return {
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = `<div style="font-weight:bold">${params[0].axisValue}</div>`;
            params.forEach(param => {
              let color = param.seriesName === '预测值' ? '#8884d8' :
                  param.seriesName === '下限' ? '#82ca9d' : '#ff7300';
              result += `<div style="color:${color}">
                          ${param.seriesName}: ${param.value.toFixed(1)}
                         </div>`;
            });
            return result;
          }
        },
        legend: {
          data: ['预测值', '下限', '上限']
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100
          },
          {
            start: 0,
            end: 100
          }
        ],
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.data.map(item => item.ds),
          axisLabel: {
            interval: 8,
            rotate: -45
          }
        },
        yAxis: {
          type: 'value',
          min: -2,
          max: 36,
          name: '预测销量'
        },
        series: [
          {
            name: '预测值',
            type: 'line',
            sampling: 'average',
            data: this.data.map(item => item.yhat),
            areaStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [{
                  offset: 0, color: 'rgba(136, 132, 216, 0.1)'
                }, {
                  offset: 1, color: 'rgba(136, 132, 216, 0)'
                }]
              }
            },
            lineStyle: {
              width: 2,
              color: '#8884d8'
            },
            symbol: 'none'
          },
          {
            name: '下限',
            type: 'line',
            sampling: 'average',
            data: this.data.map(item => item.yhat_lower),
            lineStyle: {
              width: 1,
              color: '#82ca9d',
              type: 'dashed'
            },
            symbol: 'none'
          },
          {
            name: '上限',
            type: 'line',
            sampling: 'average',
            data: this.data.map(item => item.yhat_upper),
            lineStyle: {
              width: 1,
              color: '#ff7300',
              type: 'dashed'
            },
            symbol: 'none'
          }
        ]
      };
    },

    // 月度图表配置
    monthlyChartOption() {
      return {
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['月均预测', '月均下限', '月均上限']
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.monthlyAverages.map(item => item.month)
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 30
        },
        series: [
          {
            name: '月均预测',
            type: 'line',
            data: this.monthlyAverages.map(item => item.yhat),
            lineStyle: {
              width: 2,
              color: '#8884d8'
            }
          },
          {
            name: '月均下限',
            type: 'line',
            data: this.monthlyAverages.map(item => item.yhat_lower),
            lineStyle: {
              width: 1,
              color: '#82ca9d',
              type: 'dashed'
            }
          },
          {
            name: '月均上限',
            type: 'line',
            data: this.monthlyAverages.map(item => item.yhat_upper),
            lineStyle: {
              width: 1,
              color: '#ff7300',
              type: 'dashed'
            }
          }
        ]
      };
    },

    // 置信区间图表配置
    intervalChartOption() {
      return {
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100
          },
          {
            start: 0,
            end: 100
          }
        ],
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.intervalData.map(item => item.ds),
          axisLabel: {
            interval: 8,
            rotate: -45
          }
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 20
        },
        series: [
          {
            name: '置信区间宽度',
            type: 'line',
            sampling: 'average',
            data: this.intervalData.map(item => item.interval_width),
            lineStyle: {
              width: 2,
              color: '#ff7300'
            },
            symbol: 'none'
          }
        ]
      };
    }
  }
};
</script>

