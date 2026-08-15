<script setup>
import { computed } from 'vue'

const props = defineProps({
  shop: { type: Object, required: true }
})

const distanceText = computed(() => {
  const km = props.shop.distanceKm
  if (km == null) return '—'
  if (km < 1) return `距你 ${Math.round(km * 1000)} m`
  return `距你 ${km.toFixed(2)} km`
})
</script>

<template>
  <div class="shop-card card rise">
    <div class="shop-top">
      <h3 class="shop-name">{{ shop.name }}</h3>
      <span class="score mono">{{ Number(shop.score).toFixed(1) }}分</span>
    </div>
    <p class="shop-addr">{{ shop.address }}</p>
    <div class="shop-meta">
      <span class="mono price">¥{{ shop.avgPrice / 100 }}</span>
      <span class="sold">已售 {{ shop.sold.toLocaleString() }}</span>
      <span class="chip dist">{{ distanceText }}</span>
    </div>
  </div>
</template>

<style scoped>
.shop-card {
  padding: 18px 20px;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.shop-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
  border-color: var(--green);
}

.shop-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.shop-name {
  margin: 0;
  font-size: 16.5px;
  font-weight: 800;
  letter-spacing: 0.01em;
}

.score {
  color: var(--vermilion);
  font-weight: 700;
  font-size: 15px;
  white-space: nowrap;
}

.shop-addr {
  margin: 6px 0 14px;
  font-size: 12.5px;
  color: var(--ink-faint);
}

.shop-meta {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.price {
  font-weight: 700;
  color: var(--green);
  font-size: 15px;
}

.sold {
  font-size: 12px;
  color: var(--ink-soft);
}

.dist {
  margin-left: auto;
  background: var(--green-soft);
  color: var(--green);
}
</style>
