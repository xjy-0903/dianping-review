<script setup>
import { toasts } from '../utils/toast'
</script>

<template>
  <div class="toast-host">
    <transition-group name="toast">
      <div v-for="t in toasts" :key="t.id" class="toast" :class="'toast-' + t.type">
        <span class="toast-bar"></span>
        {{ t.message }}
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed;
  top: 70px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.toast {
  position: relative;
  background: var(--ink);
  color: #f4f2ea;
  padding: 11px 22px 11px 26px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 600;
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  min-width: 220px;
  text-align: center;
}

.toast-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 5px;
  background: var(--amber);
}

.toast-success .toast-bar {
  background: #4caf7d;
}

.toast-error .toast-bar {
  background: var(--vermilion);
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
