import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// TODO: 用户状态管理
// - token: 从 localStorage 读取/写入
// - role: 'GUEST' | 'STANDARD' | 'PREMIUM_VIP'
// - globalUserId: 从鉴权接口返回
// - computed: isGuest, isStandard, isPremiumVip
// - actions: setToken(), setRole(), setGlobalUserId(), logout()

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const role = ref('GUEST')
  const globalUserId = ref('')

  // TODO: computed getters

  // TODO: actions

  return { token, role, globalUserId }
})
