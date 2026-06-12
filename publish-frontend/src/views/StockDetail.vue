<template>
  <div class="stock-detail">
    <!-- TODO: 个股详情页 - STANDARD+ 可见 -->
    <!-- 1. 实时盘口面板：最新价、涨跌幅、买一/卖一、挂单量 -->
    <!-- 2. 主力动向面板：大买家 Top1 + 大卖家 Top1 -->
    <!-- 3. setInterval 每5秒调用 getQuote()，onUnmounted 时 clearInterval -->
    <!-- 4. 基础日K线图（KLineChart 组件） -->
    <!-- 5. GUEST 仅看价格，无盘口/主力/图表 -->
    <!-- 6. STANDARD 看全部但 K线仅1D，其他周期显示 UpgradeModal -->
    <p>个股详情 (StockDetail) - 待实现</p>
  </div>
</template>

<script setup>
// TODO: 从 route.params 获取 stockCode
// TODO: 5秒轮询 getQuote()
// TODO: 权限分层渲染
</script>
