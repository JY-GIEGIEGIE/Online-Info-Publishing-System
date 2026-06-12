<template>
  <div class="portal">
    <!-- TODO: 首页门户 - 游客可见。本页面就是信息发布系统的首页，无需跳转到自己 -->
    <!--
      布局（导航栏由 App.vue 统一提供，Portal 只负责页面内容区）：
      ┌──────────────────────────────────────────────┐
      │ App.vue导航：信息发布logo | [交易系统][账户系统] | [登录] │
      ├──────────────────────────────────────────────┤
      │  搜索框（代码/拼音，调用 searchStock API）        │
      ├──────────────────┬───────────────────────────┤
      │ 大盘指数          │ 实时行情列表（5秒轮询）       │
      │ 上证 +1.2%       │ 代码  名称  最新价  涨跌幅   │
      │ 深证 -0.3%       │ 600519 贵州茅台 ... 点击进入  │
      │                  │ 000001 平安银行 ... 详情页   │
      └──────────────────┴───────────────────────────┘

      功能清单：
      1. 顶部导航：本系统 logo，其他子系统链接（交易系统/账户系统），登录/注册按钮
         - 登录点击 → 重定向到外部 SSO 账户系统
         - 已登录 → 显示角色标签 + 退出按钮
      2. 搜索框：输入股票代码或拼音缩写 → 调用 GET /stock/search
      3. 大盘指数面板：Mock 展示上证/深证指数
      4. 实时行情列表：5秒 setInterval 轮询 /quote 接口，点击行跳转 /stock/:code
      5. onUnmounted 中 clearInterval 防内存泄漏
    -->
    <p>首页 (Portal) - 待实现</p>
  </div>
</template>

<script setup>
// TODO: 引入 useUserStore, searchStock, getQuote
// TODO: setInterval 5秒轮询 + onUnmounted clearInterval
// TODO: 登录跳转 SSO、退出清除 token
</script>
