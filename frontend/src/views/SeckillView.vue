<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { user, requireLogin } from '../store/user'
import { toast } from '../utils/toast'
import TicketCard from '../components/TicketCard.vue'

const vouchers = ref([])
const stocks = reactive({})
const orders = ref([])
const grabbing = reactive({})
const ordersLoading = ref(false)

const POLL_INTERVAL = 2000
let pollTimer = null

async function loadVouchers() {
  try {
    vouchers.value = await api.seckillList()
    for (const v of vouchers.value) {
      const s = await api.voucherStock(v.id)
      stocks[v.id] = s === null ? 0 : s
    }
  } catch (e) {
    toast(e.message, 'error')
  }
}

async function loadOrders() {
  if (!user.id) return
  ordersLoading.value = true
  try {
    orders.value = await api.myOrders()
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    ordersLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadVouchers(), loadOrders()])
}

async function grab(voucher) {
  if (!requireLogin()) return
  if (grabbing[voucher.id]) return
  grabbing[voucher.id] = true
  try {
    const orderId = await api.seckill(voucher.id)
    toast(`抢购成功!订单号 ${orderId}`, 'success')
    const s = await api.voucherStock(voucher.id)
    stocks[voucher.id] = s === null ? 0 : s
    loadOrders()
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    grabbing[voucher.id] = false
  }
}

const seckillList = () => vouchers.value.filter((v) => v.type === 1)

const statusText = (s) => ({ 0: '待支付', 1: '已支付', 2: '已核销', 3: '已取消' }[s] ?? '未知')

onMounted(() => {
  refreshAll()
  pollTimer = setInterval(() => {
    for (const v of seckillList()) {
      api.voucherStock(v.id).then((s) => {
        stocks[v.id] = s === null ? 0 : s
      })
    }
  }, POLL_INTERVAL)
})

onBeforeUnmount(() => clearInterval(pollTimer))
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">限时秒杀</h1>
        <p class="page-sub">Lua 原子预扣库存 · Redis Stream 异步落库 · 库存每 2 秒实时刷新</p>
      </div>
      <button class="btn btn-ghost" @click="refreshAll">刷新</button>
    </div>

    <div class="layout">
      <div class="tickets">
        <div v-if="seckillList().length" class="ticket-list">
          <TicketCard
            v-for="v in seckillList()"
            :key="v.id"
            :voucher="v"
            :stock="stocks[v.id] ?? 0"
            :busy="!!grabbing[v.id]"
            @grab="grab"
          />
        </div>
        <div v-else class="empty">
          <strong>暂无秒杀活动</strong>
          可在后端添加秒杀券:POST /api/voucher/seckill
        </div>
      </div>

      <div class="orders card">
        <div class="orders-head">
          <h3>我的订单</h3>
          <span class="chip dist">异步落库</span>
        </div>
        <div v-if="!user.id" class="order-login">
          <p>登录后查看你的抢购订单</p>
          <button class="btn btn-primary btn-sm" @click="user.loginDialogOpen = true">去登录</button>
        </div>
        <template v-else>
          <table v-if="orders.length" class="order-table">
            <thead>
              <tr>
                <th>订单号</th>
                <th>券</th>
                <th>状态</th>
                <th>下单时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in orders" :key="o.id">
                <td class="mono oid">{{ o.id }}</td>
                <td class="mono">#{{ o.voucherId }}</td>
                <td><span class="chip status" :class="'s' + o.status">{{ statusText(o.status) }}</span></td>
                <td class="mono time">{{ o.createTime }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="order-empty">还没有订单,去抢一张试试手气</div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
  align-items: start;
}

.ticket-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.orders {
  padding: 20px;
  position: sticky;
  top: 80px;
}

.orders-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.orders-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
}

.dist {
  background: var(--green-soft);
  color: var(--green);
  font-family: var(--mono);
  font-size: 11px;
}

.order-login {
  text-align: center;
  padding: 26px 0;
  color: var(--ink-soft);
  font-size: 13px;
}

.order-login p {
  margin: 0 0 12px;
}

.order-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.order-table th {
  text-align: left;
  font-size: 11.5px;
  color: var(--ink-faint);
  font-weight: 700;
  padding: 6px 8px;
  border-bottom: 1px solid var(--mist);
}

.order-table td {
  padding: 9px 8px;
  border-bottom: 1px solid var(--mist);
  color: var(--ink);
}

.oid {
  font-size: 12px;
  color: var(--ink-soft);
}

.time {
  font-size: 11.5px;
  color: var(--ink-faint);
}

.status {
  font-size: 11px;
}

.s0 {
  background: var(--amber-soft);
  color: #b07818;
}

.s1 {
  background: var(--green-soft);
  color: var(--green);
}

.order-empty {
  padding: 26px 0;
  text-align: center;
  color: var(--ink-faint);
  font-size: 13px;
}

@media (max-width: 1000px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .orders {
    position: static;
  }
}
</style>
