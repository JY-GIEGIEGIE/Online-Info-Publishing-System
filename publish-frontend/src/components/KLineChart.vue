<template>
  <div class="kline-chart">
    <!-- TODO: ECharts K线蜡烛图组件 -->
    <!-- 1. 接收 props: klineData (KLineDTO数组) -->
    <!-- 2. 绘制阴阳蜡烛图：close>=open 红色(阳), close<open 绿色(阴) -->
    <!-- 3. 叠加 MA5(白线)、MA10(黄线) -->
    <!-- 4. 底部副图：MACD柱状图 (DIF, DEA, 红绿柱) -->
    <!-- 5. window resize 时调用 chart.resize() -->
    <!-- 6. 支持周期切换按钮：5M | 1H | 1D | 1M | 1Y -->
    <!-- 7. 非VIP点击非1D周期时 emit('need-upgrade') -->
    <p>K线图 (KLineChart) - 待实现</p>
  </div>
</template>

<script setup>
// TODO: import * as echarts from 'echarts'
// TODO: props: klineData, period
// TODO: watch data变化 → setOption()
// TODO: onMounted → init chart; onUnmounted → dispose chart
</script>
