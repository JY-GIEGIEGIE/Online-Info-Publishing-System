<template>
  <!-- 模糊由 props.visible 控制，和弹窗组件入参命名统一 -->
  <div class="kline-chart" :class="{ blur: visible }">
    <div class="period-buttons">
      <button
        v-for="item in periodList"
        :key="item.key"
        :class="['period-btn', { active: period === item.key }]"
        @click="handlePeriodClick(item.key)"
      >
        {{ item.label }}
      </button>
    </div>
    <div ref="chartRef" class="chart-box"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useUserStore } from '../stores/user'

const props = defineProps({
  klineData: {
    type: Array,
    default: () => []
  },
  period: {
    type: String,
    default: '1D'
  },
  // 统一命名 visible，和 UpgradeModal 入参同名
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['change-period', 'need-upgrade'])
const userStore = useUserStore()

const periodList = [
  { key: '5M', label: '5M' },
  { key: '1H', label: '1H' },
  { key: '1D', label: '1D' },
  { key: '1M', label: '1M' },
  { key: '1Y', label: '1Y' }
]

const chartRef = ref(null)
let chartInstance = null

const handlePeriodClick = (targetKey) => {
  if (targetKey === '1D') {
    emit('change-period', targetKey)
    return
  }
  if (!userStore.isPremiumVip) {
    emit('need-upgrade')
    return
  }
  emit('change-period', targetKey)
}

function calcMA(data, day) {
  const res = []
  for (let i = 0; i < data.length; i++) {
    if (i < day - 1) {
      res.push(null)
      continue
    }
    let sum = 0
    for (let j = i - day + 1; j <= i; j++) {
      sum += data[j].close
    }
    res.push((sum / day).toFixed(2))
  }
  return res
}

function calcMACD(data) {
  const closeArr = data.map(item => item.close)
  const difList = []
  const deaList = []
  const macdBar = []
  let ema12 = closeArr[0]
  let ema26 = closeArr[0]
  let dea = 0
  for (let i = 0; i < closeArr.length; i++) {
    ema12 = ema12 * 11 / 13 + closeArr[i] * 2 / 13
    ema26 = ema26 * 25 / 27 + closeArr[i] * 2 / 27
    const dif = ema12 - ema26
    dea = dea * 8 / 10 + dif * 2 / 10
    difList.push(dif)
    deaList.push(dea)
    macdBar.push((dif - dea) * 2)
  }
  return { difList, deaList, macdBar }
}

function initChart() {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  renderChart()
  window.addEventListener('resize', handleChartResize)
}

function handleChartResize() {
  chartInstance?.resize()
}

function renderChart() {
  if (!chartInstance || !props.klineData.length) return
  const data = props.klineData

  const xData = data.map(item => item.time)
  const kData = data.map(item => [item.open, item.close, item.low, item.high])
  const ma5 = calcMA(data, 5)
  const ma10 = calcMA(data, 10)
  const { difList, deaList, macdBar } = calcMACD(data)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['K线', 'MA5', 'MA10', 'DIF', 'DEA', 'MACD'],
      top: 0
    },
    grid: [
      { left: '3%', right: '4%', top: '12%', height: '60%' },
      { left: '3%', right: '4%', top: '75%', height: '18%' }
    ],
    xAxis: [
      {
        type: 'category',
        data: xData,
        boundaryGap: false,
        axisLine: { onZero: false },
        splitLine: { show: false },
        axisLabel: { show: false }
      },
      {
        gridIndex: 1,
        type: 'category',
        data: xData,
        boundaryGap: false,
        axisLine: { onZero: false },
        splitLine: { show: false }
      }
    ],
    yAxis: [
      { scale: true, splitLine: { lineStyle: { type: 'dashed' } } },
      { gridIndex: 1, scale: true }
    ],
    series: [
      {
        name: 'K线',
        type: 'candlestick',
        xAxisIndex: 0,
        yAxisIndex: 0,
        data: kData,
        itemStyle: {
          color: '#ef4444',
          color0: '#22c55e',
          borderColor: '#ef4444',
          borderColor0: '#22c55e'
        }
      },
      {
        name: 'MA5',
        type: 'line',
        xAxisIndex: 0,
        yAxisIndex: 0,
        data: ma5,
        lineStyle: { color: '#ffffff', width: 1.5 },
        symbol: 'none'
      },
      {
        name: 'MA10',
        type: 'line',
        xAxisIndex: 0,
        yAxisIndex: 0,
        data: ma10,
        lineStyle: { color: '#eab308', width: 1.5 },
        symbol: 'none'
      },
      {
        name: 'DIF',
        type: 'line',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: difList,
        lineStyle: { color: '#0ea5e9', width: 1.2 },
        symbol: 'none'
      },
      {
        name: 'DEA',
        type: 'line',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: deaList,
        lineStyle: { color: '#f97316', width: 1.2 },
        symbol: 'none'
      },
      {
        name: 'MACD',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: macdBar,
        itemStyle: {
          color: '#ef4444',
          color0: '#22c55e'
        }
      }
    ]
  }
  chartInstance.setOption(option)
}

watch(() => props.klineData, () => {
  nextTick(() => renderChart())
}, { deep: true })

onMounted(() => {
  initChart()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleChartResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.kline-chart {
  width: 100%;
  height: 100%;
}
.blur {
  filter: blur(5px);
  pointer-events: none;
}
.period-buttons {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}
.period-btn {
  padding: 6px 14px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.period-btn:hover {
  border-color: #c8102e;
}
.period-btn.active {
  background: #c8102e;
  color: #fff;
  border-color: #c8102e;
}
.chart-box {
  width: 100%;
  height: 620px;
  border: 1px solid #eee;
}
</style>