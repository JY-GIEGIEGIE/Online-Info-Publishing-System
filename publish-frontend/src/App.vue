<template>
  <div id="app" class="app-shell">
    <header class="topbar">
      <router-link class="brand" to="/">网上信息发布</router-link>

      <nav class="subsystem-links">
        <a href="https://trade.example.com" target="_blank" rel="noopener noreferrer">交易系统</a>
        <a href="https://account.example.com" target="_blank" rel="noopener noreferrer">账户系统</a>
      </nav>

      <div class="actions">
        <span v-if="userStore.token" class="role-pill">{{ roleLabel }}</span>
        <button v-if="!userStore.token" class="ghost-btn" type="button" @click="handleLogin">登录(STANDARD)</button>
        <button v-if="!userStore.token" class="primary-btn" type="button" @click="handleVipLogin">登录(VIP)</button>
        <button v-else class="ghost-btn" type="button" @click="handleLogout">退出</button>
      </div>
    </header>

    <main class="page-shell">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from './stores/user'

const router = useRouter()
const userStore = useUserStore()

const roleLabel = computed(() => {
  if (userStore.isPremiumVip) return 'PREMIUM_VIP'
  if (userStore.isStandard) return 'STANDARD'
  return 'GUEST'
})

const handleLogin = () => {
  // Mock SSO 登录：写入 token，AuthInterceptor 识别为 STANDARD（sec_acc_no=S0001）
  userStore.setToken('valid_token')
  userStore.setRole('STANDARD')
  userStore.setGlobalUserId('F0001')
  router.push('/')
}

const handleVipLogin = () => {
  // Mock VIP 登录（vip_token → AuthInterceptor 返回 fundAccNo=F0002，DB 里 is_premium=true）
  userStore.setToken('vip_token')
  userStore.setRole('PREMIUM_VIP')
  userStore.setGlobalUserId('F0002')
  router.push('/')
}

const handleLogout = () => {
  userStore.logout()
  router.push('/')
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@400;600;700&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400;500;600&display=swap');

.app-shell {
  min-height: 100vh;
  background: #F8F9FA;
  color: #1b1c1c;
  font-family: 'Inter', sans-serif;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid #E8E8E8;
  background: #ffffff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand {
  font-size: 1.25rem;
  font-weight: 700;
  color: #b7000c;
  font-family: 'IBM Plex Sans', sans-serif;
  text-decoration: none;
}

.subsystem-links {
  display: flex;
  gap: 12px;
}

.subsystem-links a {
  color: #666666;
  text-decoration: none;
  font-size: 0.95rem;
  font-weight: 500;
  transition: color 0.2s;
}

.subsystem-links a:hover {
  color: #b7000c;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.role-pill,
.primary-btn,
.ghost-btn {
  border-radius: 4px;
  padding: 8px 12px;
  border: 1px solid transparent;
  font-size: 0.92rem;
  cursor: pointer;
}

.role-pill {
  background: #ffdad5;
  border-color: #ffb4aa;
  color: #410001;
  font-weight: 600;
}

.primary-btn {
  background: #b7000c;
  color: #ffffff;
  font-weight: 700;
}

.ghost-btn {
  background: #fbf9f8;
  color: #1b1c1c;
  border-color: #E8E8E8;
}

.ghost-btn:hover {
  background: #f6f3f2;
}

.page-shell {
  padding: 24px;
}
</style>
