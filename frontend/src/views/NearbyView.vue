<script setup>
import { reactive, ref } from 'vue'
import { api } from '../api'
import { toast } from '../utils/toast'
import ShopCard from '../components/ShopCard.vue'

const form = reactive({ x: '116.404', y: '39.915', distance: 5 })
const shops = ref([])
const loading = ref(false)
const searched = ref(false)

async function search() {
  loading.value = true
  try {
    shops.value = await api.nearby({
      x: Number(form.x),
      y: Number(form.y),
      distance: Number(form.distance)
    })
    searched.value = true
    if (!shops.value.length) toast('范围内暂无商户,试试调大搜索半径')
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

const detail = ref(null)
const latency = ref(null)
const secondLatency = ref(null)
const detailLoading = ref(false)

async function openDetail(shop) {
  detail.value = shop
  latency.value = null
  secondLatency.value = null
  detailLoading.value = true
  const t0 = performance.now()
  try {
    detail.value = await api.shopDetail(shop.id)
    latency.value = Math.round(performance.now() - t0)
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    detailLoading.value = false
  }
}

async function queryAgain() {
  if (!detail.value) return
  const t0 = performance.now()
  try {
    await api.shopDetail(detail.value.id)
    secondLatency.value = Math.round(performance.now() - t0)
  } catch (e) {
    toast(e.message, 'error')
  }
}
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">附近商户</h1>
        <p class="page-sub">Redis GEO 地理检索 · 按距离升序,默认坐标天安门</p>
      </div>
    </div>

    <div class="card ctrl">
      <div class="field">
        <label>经度 X</label>
        <input v-model="form.x" inputmode="decimal" class="mono" />
      </div>
      <div class="field">
        <label>纬度 Y</label>
        <input v-model="form.y" inputmode="decimal" class="mono" />
      </div>
      <div class="field range-field">
        <label>范围 <span class="mono range-val">{{ form.distance }} km</span></label>
        <input v-model="form.distance" type="range" min="0.5" max="20" step="0.5" />
      </div>
      <button class="btn btn-primary go" :disabled="loading" @click="search">
        {{ loading ? '搜索中…' : '找附近' }}
      </button>
    </div>

    <div v-if="loading" class="grid">
      <div v-for="i in 6" :key="i" class="card skeleton"></div>
    </div>

    <div v-else-if="searched && shops.length" class="grid">
      <ShopCard v-for="shop in shops" :key="shop.id" :shop="shop" @click="openDetail(shop)" />
    </div>

    <div v-else-if="searched" class="empty">
      <strong>范围内暂无商户</strong>
      换个坐标或把搜索半径调大一点
    </div>

    <div v-else class="grid intro">
      <div class="card intro-card rise">
        <h3>怎么玩</h3>
        <p>输入任意坐标与半径,点「找附近」,商户按距离由近到远排列。</p>
        <p>点开任意商户详情,连续查两次,看看第二次的响应时间变化 —— 那是多级缓存在起作用。</p>
        <button class="btn btn-ghost" @click="search">先试试天安门附近</button>
      </div>
    </div>

    <transition name="fade">
      <div v-if="detail" class="mask" @click.self="detail = null">
        <div class="card detail rise">
          <div class="detail-head">
            <h2>{{ detail.name }}</h2>
            <button class="close" aria-label="关闭" @click="detail = null">✕</button>
          </div>
          <p class="detail-addr">{{ detail.address }} · {{ detail.openHours }}</p>

          <div class="detail-stats">
            <div class="stat">
              <div class="stat-num mono">{{ detail.score }}<span class="stat-unit">分</span></div>
              <div class="stat-label">评分</div>
            </div>
            <div class="stat">
              <div class="stat-num mono">¥{{ detail.avgPrice / 100 }}</div>
              <div class="stat-label">人均</div>
            </div>
            <div class="stat">
              <div class="stat-num mono">{{ detail.sold.toLocaleString() }}</div>
              <div class="stat-label">已售</div>
            </div>
            <div class="stat">
              <div class="stat-num mono">{{ detail.comments.toLocaleString() }}</div>
              <div class="stat-label">评论</div>
            </div>
          </div>

          <div class="latency-row">
            <span class="chip lat-chip mono">首次查询 {{ detailLoading ? '…' : latency !== null ? latency + ' ms' : '—' }}</span>
            <span class="chip lat-chip mono" v-if="secondLatency !== null">
              第二次查询 {{ secondLatency }} ms
            </span>
            <span v-if="latency !== null && secondLatency !== null" class="hit-tip">
              第二次明显更快:Cache Aside 命中缓存,未回源数据库
            </span>
          </div>

          <button class="btn btn-primary again" :disabled="detailLoading" @click="queryAgain">再查一次(验证缓存)</button>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.ctrl {
  display: grid;
  grid-template-columns: 160px 160px 1fr auto;
  gap: 16px;
  align-items: end;
  padding: 18px 20px;
  margin-bottom: 22px;
}

.range-val {
  color: var(--vermilion);
  font-weight: 700;
}

.go {
  height: 40px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 16px;
}

.skeleton {
  height: 132px;
  background: linear-gradient(100deg, var(--card) 40%, var(--mist) 50%, var(--card) 60%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}

.intro-card {
  padding: 24px;
}

.intro-card h3 {
  margin: 0 0 10px;
  font-size: 17px;
}

.intro-card p {
  color: var(--ink-soft);
  font-size: 13.5px;
  margin: 0 0 8px;
}

.intro-card .btn {
  margin-top: 8px;
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(20, 26, 23, 0.5);
  display: grid;
  place-items: center;
  z-index: 60;
  padding: 20px;
}

.detail {
  width: min(520px, 100%);
  padding: 26px;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.detail-head h2 {
  margin: 0;
  font-size: 21px;
  font-weight: 800;
}

.close {
  border: none;
  background: transparent;
  color: var(--ink-faint);
  font-size: 16px;
  padding: 4px;
}

.close:hover {
  color: var(--ink);
}

.detail-addr {
  color: var(--ink-faint);
  font-size: 12.5px;
  margin: 6px 0 20px;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 18px;
}

.stat {
  background: var(--paper);
  border: 1px solid var(--mist);
  border-radius: 12px;
  padding: 12px 8px;
  text-align: center;
}

.stat-num {
  font-size: 17px;
  font-weight: 700;
  color: var(--green);
}

.stat-unit {
  font-size: 11px;
  color: var(--ink-faint);
}

.stat-label {
  font-size: 11px;
  color: var(--ink-soft);
  margin-top: 2px;
}

.latency-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.lat-chip {
  background: var(--green-soft);
  color: var(--green);
}

.hit-tip {
  font-size: 12px;
  color: var(--ink-soft);
}

.again {
  width: 100%;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
