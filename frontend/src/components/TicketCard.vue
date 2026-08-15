<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  voucher: { type: Object, required: true },
  stock: { type: Number, default: 0 },
  busy: { type: Boolean, default: false }
})

const emit = defineEmits(['grab'])

const now = ref(Date.now())
const timer = setInterval(() => (now.value = Date.now()), 1000)
onBeforeUnmount(() => clearInterval(timer))

const endMs = computed(() => {
  if (!props.voucher.endTime) return 0
  return new Date(props.voucher.endTime.replace(' ', 'T')).getTime()
})
const countdown = computed(() => {
  const diff = Math.max(0, endMs.value - now.value)
  const h = String(Math.floor(diff / 3600000)).padStart(2, '0')
  const m = String(Math.floor((diff % 3600000) / 60000)).padStart(2, '0')
  const s = String(Math.floor((diff % 60000) / 1000)).padStart(2, '0')
  return `${h}:${m}:${s}`
})
const urgent = computed(() => endMs.value - now.value < 60000)

const total = computed(() => Math.max(props.voucher.stock || 0, 1))
const pct = computed(() => Math.max(0, Math.min(100, (props.stock / total.value) * 100)))
const state = computed(() => {
  if (props.stock <= 0) return 'soldout'
  if (pct.value < 10) return 'scarce'
  if (pct.value < 50) return 'low'
  return 'ok'
})

const stateText = computed(() => {
  if (state.value === 'soldout') return '已售罄'
  if (state.value === 'scarce') return '即将售罄'
  if (state.value === 'low') return '库存紧张'
  return '库存充足'
})

watch(
  () => props.stock,
  (nv, ov) => {
    if (ov !== undefined && nv < ov) {
      flash.value = true
      setTimeout(() => (flash.value = false), 500)
    }
  }
)

const flash = ref(false)
</script>

<template>
  <div class="ticket rise" :class="[state, { busy }]">
    <div v-if="state === 'soldout'" class="stamp">已售罄</div>

    <div class="ticket-body">
      <div class="ticket-head">
        <h3 class="t-title">{{ voucher.title }}</h3>
        <span class="t-sub">{{ voucher.subTitle }}</span>
      </div>

      <div class="t-prices">
        <span class="pay mono">¥{{ voucher.payValue / 100 }}</span>
        <span class="actual mono">抵 ¥{{ voucher.actualValue / 100 }}</span>
      </div>

      <p class="t-rules">{{ voucher.rules }}</p>

      <div class="stock-line">
        <div class="stock-text">
          <span class="stock-num mono" :class="{ flash }">{{ stock }}</span>
          <span class="stock-total mono">/ {{ voucher.stock }}</span>
          <span class="stock-state">{{ stateText }}</span>
        </div>
        <div class="pulse">
          <div class="pulse-fill" :class="state" :style="{ width: pct + '%' }"></div>
        </div>
      </div>
    </div>

    <div class="ticket-stub">
      <div class="count mono" :class="{ urgent }">{{ countdown }}</div>
      <div class="count-label">距结束</div>
      <button
        class="btn btn-danger grab"
        :disabled="state === 'soldout' || busy"
        @click="emit('grab', voucher)"
      >
        {{ busy ? '抢购中' : state === 'soldout' ? '来晚了' : '抢' }}
      </button>
    </div>

    <span class="perf perf-t"></span>
    <span class="perf perf-b"></span>
  </div>
</template>

<style scoped>
.ticket {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 132px;
  background: var(--card);
  border: 1px solid var(--mist);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.ticket:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}

.ticket-body {
  padding: 20px 22px;
}

.ticket-stub {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-left: 2px dashed var(--mist);
  background: var(--paper);
  padding: 16px 14px;
}

.t-title {
  margin: 0;
  font-size: 17px;
  font-weight: 800;
}

.t-sub {
  font-size: 12px;
  color: var(--ink-faint);
}

.t-prices {
  margin: 12px 0 6px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.pay {
  font-size: 34px;
  font-weight: 700;
  color: var(--vermilion);
  line-height: 1;
}

.actual {
  font-size: 14px;
  color: var(--ink-soft);
}

.t-rules {
  margin: 0 0 14px;
  font-size: 12px;
  color: var(--ink-faint);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.stock-line {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stock-text {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stock-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--ink);
  transition: color 0.3s ease;
}

.stock-num.flash {
  color: var(--vermilion);
}

.stock-total {
  font-size: 12px;
  color: var(--ink-faint);
}

.stock-state {
  margin-left: auto;
  font-size: 12px;
  font-weight: 700;
}

.stock-state {
  color: var(--green);
}

.scarce .stock-state,
.low .stock-state {
  color: var(--amber);
}

.soldout .stock-state {
  color: var(--ink-faint);
}

.pulse {
  height: 8px;
  border-radius: 999px;
  background: var(--mist);
  overflow: hidden;
}

.pulse-fill {
  height: 100%;
  border-radius: 999px;
  background: var(--green);
  transition: width 0.6s ease;
}

.pulse-fill.low {
  background: var(--amber);
}

.pulse-fill.scarce {
  background: var(--vermilion);
  animation: beat 1.2s ease infinite;
}

@keyframes beat {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}

.count {
  font-size: 24px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.02em;
}

.count.urgent {
  color: var(--vermilion);
}

.count-label {
  font-size: 11px;
  color: var(--ink-faint);
  margin-bottom: 8px;
}

.grab {
  width: 92px;
  padding: 10px 0;
  font-size: 16px;
}

.soldout {
  opacity: 0.82;
}

.stamp {
  position: absolute;
  top: 12px;
  right: -6px;
  transform: rotate(8deg);
  border: 2.5px solid var(--vermilion);
  color: var(--vermilion);
  font-weight: 800;
  font-size: 13px;
  letter-spacing: 0.2em;
  padding: 3px 10px;
  border-radius: 6px;
  opacity: 0.85;
  z-index: 5;
  background: rgba(255, 253, 246, 0.72);
}

.perf {
  position: absolute;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--paper);
  border: 1px solid var(--mist);
  left: -10px;
}

.perf-t {
  top: -10px;
}

.perf-b {
  bottom: -10px;
}

.busy {
  pointer-events: none;
}
</style>
