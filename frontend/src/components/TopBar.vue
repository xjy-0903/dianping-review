<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { user, logout, requireLogin } from '../store/user'

const route = useRoute()
const title = computed(() => route.meta.title || '')

function onUserClick() {
  if (user.id) {
    logout()
  } else {
    user.loginDialogOpen = true
  }
}
</script>

<template>
  <header class="topbar">
    <div class="crumb">
      <span class="crumb-root">巷评</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-here">{{ title }}</span>
    </div>
    <div class="user-zone">
      <template v-if="user.id">
        <span class="hello">你好,<strong>{{ user.nickName }}</strong></span>
        <span class="uid mono">#{{ user.id }}</span>
        <button class="btn btn-ghost btn-sm" @click="onUserClick">退出</button>
      </template>
      <button v-else class="btn btn-primary btn-sm" @click="onUserClick">登录 / 注册</button>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  border-bottom: 1px solid var(--mist);
  background: rgba(246, 242, 233, 0.86);
  backdrop-filter: blur(6px);
  position: sticky;
  top: 0;
  z-index: 20;
}

.crumb {
  font-size: 14px;
  color: var(--ink-soft);
}

.crumb-root {
  font-weight: 800;
  color: var(--ink);
}

.crumb-sep {
  margin: 0 8px;
  color: var(--ink-faint);
}

.crumb-here {
  font-weight: 700;
  color: var(--green);
}

.user-zone {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hello {
  font-size: 13px;
  color: var(--ink-soft);
}

.hello strong {
  color: var(--ink);
}

.uid {
  font-size: 12px;
  color: var(--ink-faint);
  background: var(--mist);
  padding: 2px 8px;
  border-radius: 999px;
}

.btn-sm {
  padding: 6px 14px;
  font-size: 13px;
}
</style>
