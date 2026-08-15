<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'

const props = defineProps({
  month: { type: Object, required: true }
})

const now = ref(new Date())
const year = computed(() => now.value.getFullYear())
const monthIdx = computed(() => now.value.getMonth())
const daysInMonth = computed(() => new Date(year.value, monthIdx.value + 1, 0).getDate())
const firstWeekday = computed(() => {
  const d = new Date(year.value, monthIdx.value, 1).getDay()
  return d === 0 ? 6 : d - 1
})
const today = computed(() => {
  const t = new Date()
  return t.getFullYear() === year.value && t.getMonth() === monthIdx.value ? t.getDate() : 0
})
const cells = computed(() => {
  const arr = []
  for (let i = 0; i < firstWeekday.value; i++) arr.push(null)
  for (let d = 1; d <= daysInMonth.value; d++) arr.push(d)
  return arr
})

onMounted(() => {
  const t = new Date()
  now.value = new Date(t.getFullYear(), t.getMonth(), 1)
})
</script>

<template>
  <div class="calendar">
    <div class="cal-head">
      <span class="cal-title">{{ year }} 年 {{ monthIdx + 1 }} 月</span>
      <span class="cal-tip">BitMap 存储 · 一年约 46B</span>
    </div>
    <div class="cal-grid cal-week">
      <span v-for="w in ['一', '二', '三', '四', '五', '六', '日']" :key="w">{{ w }}</span>
    </div>
    <div class="cal-grid">
      <template v-for="(d, i) in cells" :key="i">
        <span v-if="d === null" class="cal-cell blank"></span>
        <span
          v-else
          class="cal-cell"
          :class="{
            signed: props.month[d],
            today: d === today,
            future: d > today
          }"
        >
          <span class="cal-day">{{ d }}</span>
        </span>
      </template>
    </div>
  </div>
</template>

<style scoped>
.calendar {
  padding: 20px 22px 24px;
}

.cal-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
}

.cal-title {
  font-size: 16px;
  font-weight: 800;
}

.cal-tip {
  font-size: 11px;
  color: var(--ink-faint);
  font-family: var(--mono);
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.cal-week {
  margin-bottom: 6px;
}

.cal-week span {
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-faint);
  padding: 4px 0;
}

.cal-cell {
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  border-radius: 10px;
  font-size: 13px;
  color: var(--ink-soft);
  background: transparent;
  border: 1px solid transparent;
}

.cal-cell.signed {
  background: var(--green);
  color: #f4f7ef;
  font-weight: 700;
  box-shadow: 0 2px 0 var(--green-deep);
}

.cal-cell.today {
  border-color: var(--vermilion);
  color: var(--vermilion);
  font-weight: 700;
}

.cal-cell.today.signed {
  border-color: var(--vermilion);
  box-shadow: 0 2px 0 var(--green-deep), 0 0 0 1.5px var(--vermilion);
}

.cal-cell.future {
  color: var(--ink-faint);
  opacity: 0.55;
}
</style>
