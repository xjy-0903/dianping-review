<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { requireLogin } from '../store/user'
import { toast } from '../utils/toast'
import SignCalendar from '../components/SignCalendar.vue'

const signedDays = reactive({})
const streak = ref(0)
const todaySigned = ref(false)
const loading = ref(false)
const signing = ref(false)

const now = new Date()
const today = now.getDate()
const monthLabel = `${now.getFullYear()} 年 ${now.getMonth() + 1} 月`

const signedCount = computed(() => Object.values(signedDays).filter(Boolean).length)

async function refresh() {
  try {
    const [month, count, status] = await Promise.all([api.signMonth(), api.signCount(), api.signStatus()])
    Object.keys(signedDays).forEach((k) => delete signedDays[k])
    Object.assign(signedDays, month)
    streak.value = count
    todaySigned.value = !!status
  } catch (e) {
    toast(e.message, 'error')
  }
}

async function sign() {
  if (!requireLogin()) return
  signing.value = true
  try {
    await api.sign()
    todaySigned.value = true
    signedDays[today] = true
    streak.value = await api.signCount()
    toast('签到成功,已点亮今天', 'success')
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    signing.value = false
  }
}

onMounted(refresh)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">每日签到</h1>
        <p class="page-sub">Redis BitMap · 每位只占 1 bit,签到数据近乎零成本</p>
      </div>
    </div>

    <div class="layout">
      <div class="hero card rise">
        <div class="hero-top">
          <span class="chip today-chip" :class="{ signed: todaySigned }">
            {{ todaySigned ? '今日已签到' : '今日未签到' }}
          </span>
          <span class="hero-month">{{ monthLabel }}</span>
        </div>

        <div class="streak">
          <span class="streak-num mono">{{ streak }}</span>
          <span class="streak-unit">天</span>
        </div>
        <div class="streak-label">连续签到</div>

        <div class="hero-foot">
          <button class="btn btn-primary big" :disabled="todaySigned || signing" @click="sign">
            {{ todaySigned ? '已签到' : signing ? '签到中…' : '立即签到' }}
          </button>
          <span class="month-count">本月累计 <strong class="mono">{{ signedCount }}</strong> 天</span>
        </div>
      </div>

      <div class="card rise">
        <SignCalendar :month="signedDays" />
      </div>
    </div>

    <div class="tips">
      <div class="tip card">
        <div class="tip-num mono">01</div>
        <div>
          <h4>BitMap 存储</h4>
          <p>一天一个 bit,一位用户一年的签到只占约 46 字节,比数据库行存储节省 90% 以上。</p>
        </div>
      </div>
      <div class="tip card">
        <div class="tip-num mono">02</div>
        <div>
          <h4>BITFIELD 位运算</h4>
          <p>连续天数用一条 BITFIELD 命令取出位掩码,逐位右移统计,复杂度 O(连续天数)。</p>
        </div>
      </div>
      <div class="tip card">
        <div class="tip-num mono">03</div>
        <div>
          <h4>SETBIT 天然幂等</h4>
          <p>重复签到只会把同一位再置 1,无需加锁、无需去重。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
  align-items: start;
}

.hero {
  padding: 26px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.today-chip {
  background: var(--amber-soft);
  color: #b07818;
}

.today-chip.signed {
  background: var(--green-soft);
  color: var(--green);
}

.hero-month {
  font-size: 12px;
  color: var(--ink-faint);
}

.streak {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.streak-num {
  font-size: 76px;
  font-weight: 700;
  line-height: 1;
  color: var(--green);
  letter-spacing: -0.03em;
}

.streak-unit {
  font-size: 18px;
  color: var(--ink-soft);
  font-weight: 700;
}

.streak-label {
  color: var(--ink-faint);
  font-size: 13px;
  margin-top: -6px;
}

.hero-foot {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.big {
  padding: 13px 0;
  font-size: 16px;
}

.month-count {
  font-size: 12px;
  color: var(--ink-soft);
  text-align: center;
}

.month-count strong {
  color: var(--vermilion);
}

.tips {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 20px;
}

.tip {
  padding: 18px 20px;
  display: flex;
  gap: 14px;
}

.tip-num {
  font-size: 13px;
  font-weight: 700;
  color: var(--vermilion);
  border: 1.5px solid var(--vermilion);
  border-radius: 8px;
  height: 30px;
  min-width: 30px;
  display: grid;
  place-items: center;
}

.tip h4 {
  margin: 0 0 4px;
  font-size: 14.5px;
}

.tip p {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-soft);
}

@media (max-width: 960px) {
  .layout,
  .tips {
    grid-template-columns: 1fr;
  }
}
</style>
