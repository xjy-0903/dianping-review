<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const items = [
  {
    path: '/',
    label: '附近商户',
    sub: 'NEARBY',
    icon: 'M12 2C7.6 2 4 5.6 4 10c0 5.5 8 12 8 12s8-6.5 8-12c0-4.4-3.6-8-8-8zm0 11a3 3 0 1 1 0-6 3 3 0 0 1 0 6z'
  },
  {
    path: '/seckill',
    label: '限时秒杀',
    sub: 'SECKILL',
    icon: 'M4 5h16v3H4V5zm0 5h16v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-9zm4 3v3M9 14.5h6M12 11v6'
  },
  {
    path: '/sign',
    label: '每日签到',
    sub: 'SIGN-IN',
    icon: 'M8 3h8v4H8V3zm2 4v3M14 7v3M6 9h12v10a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V9zm4 5h4'
  },
  {
    path: '/stats',
    label: '数据统计',
    sub: 'METRICS',
    icon: 'M5 20V10M12 20V4M19 20v-8'
  }
]
</script>

<template>
  <aside class="rail">
    <div class="brand">
      <div class="brand-mark">巷</div>
      <div class="brand-text">
        <div class="brand-name">巷评</div>
        <div class="brand-sub">XIANGPING · 本地生活</div>
      </div>
    </div>

    <nav class="nav">
      <router-link v-for="item in items" :key="item.path" :to="item.path" class="nav-item" exact-active-class="active">
        <svg class="nav-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path :d="item.icon" />
        </svg>
        <span class="nav-label">{{ item.label }}</span>
        <span class="nav-sub">{{ item.sub }}</span>
      </router-link>
    </nav>

    <div class="rail-foot">
      <div class="stack-badge">
        <span class="dot dot-r"></span>Redis
        <span class="dot dot-m"></span>MySQL
      </div>
      <div class="rail-ver">Spring Boot 3 · v1.0</div>
    </div>
  </aside>
</template>

<style scoped>
.rail {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--rail);
  color: var(--rail-ink);
  padding: 24px 14px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 10px 22px;
  border-bottom: 1px solid rgba(238, 240, 228, 0.14);
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: var(--vermilion);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 22px;
  font-weight: 900;
  box-shadow: 0 3px 0 #a92b1f;
}

.brand-name {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.brand-sub {
  font-size: 10px;
  letter-spacing: 0.14em;
  color: rgba(238, 240, 228, 0.55);
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 18px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 10px;
  color: rgba(238, 240, 228, 0.72);
  text-decoration: none;
  transition: background 0.15s ease, color 0.15s ease;
  position: relative;
}

.nav-item:hover {
  background: rgba(238, 240, 228, 0.08);
  color: var(--rail-ink);
}

.nav-item.active {
  background: var(--paper);
  color: var(--ink);
  font-weight: 700;
}

.nav-item.active .nav-sub {
  color: var(--vermilion);
}

.nav-icon {
  width: 19px;
  height: 19px;
  flex: none;
  opacity: 0.9;
}

.nav-label {
  font-size: 14px;
}

.nav-sub {
  position: absolute;
  right: 12px;
  font-size: 9px;
  letter-spacing: 0.1em;
  color: rgba(238, 240, 228, 0.4);
  font-family: var(--mono);
}

.rail-foot {
  margin-top: auto;
  padding: 0 10px;
  font-size: 11px;
  color: rgba(238, 240, 228, 0.5);
}

.stack-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--mono);
  font-size: 11px;
  margin-bottom: 6px;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}

.dot-r {
  background: var(--vermilion);
  margin-left: 4px;
}

.dot-m {
  background: #4a90d9;
  margin-left: 10px;
}

.rail-ver {
  letter-spacing: 0.06em;
}

@media (max-width: 860px) {
  .brand-text,
  .nav-label,
  .nav-sub,
  .rail-ver,
  .stack-badge {
    display: none;
  }

  .rail {
    padding: 20px 10px;
  }

  .brand {
    justify-content: center;
    padding: 0 0 18px;
  }

  .nav-item {
    justify-content: center;
  }
}
</style>
