import axios from 'axios'
import { user } from '../store/user'

export const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.request.use((config) => {
  if (user.id) {
    config.headers['X-User-Id'] = String(user.id)
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && body.success === false) {
      return Promise.reject(new Error(body.errorMsg || '请求失败'))
    }
    return body ? body.data : null
  },
  (err) => {
    const msg = err?.response?.data?.errorMsg || err?.message || '网络异常'
    return Promise.reject(new Error(msg))
  }
)

export const api = {
  login: (form) => http.post('/user/login', form),
  nearby: (params) => http.get('/shop/nearby', { params }),
  shopDetail: (id) => http.get(`/shop/${id}`),
  voucherList: (shopId) => http.get(`/voucher/list/${shopId}`),
  seckillList: () => http.get('/voucher/seckill/list'),
  voucherStock: (id) => http.get(`/voucher/seckill/stock/${id}`),
  seckill: (voucherId) => http.post(`/voucher-order/seckill/${voucherId}`),
  myOrders: () => http.get('/voucher-order/user'),
  sign: () => http.post('/sign'),
  signCount: () => http.get('/sign/count'),
  signStatus: () => http.get('/sign/status'),
  signMonth: () => http.get('/sign/month'),
  recordUv: (shopId) => http.post(`/stats/uv/record/${shopId}`),
  uvCount: (shopId) => http.get(`/stats/uv/${shopId}`)
}
