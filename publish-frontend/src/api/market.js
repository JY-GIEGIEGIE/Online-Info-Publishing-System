import axios from 'axios'

// TODO: 创建 axios 实例，baseURL = '/api/publish'
// TODO: 请求拦截器：从 userStore 读取 token，注入 Authorization header
// TODO: 响应拦截器：统一解析 { code, message, data } 结构

const api = axios.create({
  baseURL: '/api/publish',
  timeout: 10000
})

export function searchStock(keyword) {
  // TODO: GET /stock/search?keyword={xx}
  throw new Error('TODO')
}

export function getQuote(stockCode) {
  // TODO: GET /market/quote/{stockCode}
  throw new Error('TODO')
}

export function getKLine(stockCode, period = '1D') {
  // TODO: GET /market/kline?stockCode={xx}&period={xx}
  throw new Error('TODO')
}

export function upgradeToVip() {
  // TODO: POST /user/upgrade
  throw new Error('TODO')
}

export default api
