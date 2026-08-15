<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'
import { toast } from '../utils/toast'

const shopId = ref('1')
const uv = ref(null)
const loading = ref(false)
const simulating = ref(false)

async function query() {
  loading.value = true
  try {
    uv.value = await api.uvCount(Number(shopId.value))
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

async function simulate() {
  simulating.value = true
  try {
    await api.recordUv(Number(shopId.value))
    await query()
    toast('已模拟一位新访客', 'success')
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    simulating.value = false
  }
}

onMounted(query)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">数据统计</h1>
        <p class="page-sub">HyperLogLog 基数统计 · 千万级 UV 仅 12KB 内存</p>
      </div>
    </div>

    <div class="layout">
      <div class="card ctrl">
        <div class="field">
          <label>商户 ID</label>
          <input v-model="shopId" inputmode="numeric" class="mono" />
        </div>
        <button class="btn btn-primary" :disabled="loading" @click="query">查询 UV</button>
      </div>

      <div class="card uv-card">
        <div class="uv-label">独立访客 UV</div>
        <div class="uv-num mono">{{ uv === null ? '—' : uv.toLocaleString() }}</div>
        <button class="btn btn-ghost" :disabled="simulating" @click="simulate">
          {{ simulating ? '模拟中…' : '模拟一位访客' }}
        </button>
      </div>

      <div class="card explain">
        <h3>为什么用 HyperLogLog?</h3>
        <p>
          精确统计 UV 需要 SET 存所有用户 ID,亿级用户占用内存以 GB 计。HyperLogLog
          用概率算法把基数压缩到 12KB,标准误差仅 0.81%,非常适合"日活/UV"这类允许微小误差的海量统计场景。
        </p>
        <p class="mono cmd">PFADD uv:shop:{{ shopId }} <user> → PFCOUNT uv:shop:{{ shopId }}</p>
        <p class="note">
          访问「附近商户」页并点开商户详情,后端会自动 PFADD 一次,回来这里查数字就会变化。
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 20px;
  align-items: start;
}

.ctrl {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.uv-card {
  padding: 26px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.uv-label {
  font-size: 13px;
  color: var(--ink-soft);
  font-weight: 700;
  letter-spacing: 0.08em;
}

.uv-num {
  font-size: 64px;
  font-weight: 700;
  line-height: 1;
  color: var(--green);
  letter-spacing: -0.02em;
}

.explain {
  padding: 22px;
  grid-column: 2;
}

.explain h3 {
  margin: 0 0 10px;
  font-size: 16px;
}

.explain p {
  font-size: 13px;
  color: var(--ink-soft);
  margin: 0 0 10px;
  line-height: 1.8;
}

.cmd {
  background: var(--rail);
  color: #c9d6c9;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 12.5px;
}

.note {
  color: var(--ink-faint);
  font-size: 12px;
}
</style>
