<script setup>
import NavRail from './components/NavRail.vue'
import TopBar from './components/TopBar.vue'
import LoginDialog from './components/LoginDialog.vue'
import ToastHost from './components/ToastHost.vue'
</script>

<template>
  <div class="shell">
    <NavRail />
    <div class="main">
      <TopBar />
      <router-view />
    </div>
    <LoginDialog />
    <ToastHost />
  </div>
</template>

<style scoped>
.shell {
  display: grid;
  grid-template-columns: 224px 1fr;
  min-height: 100vh;
}

.main {
  min-width: 0;
}

@media (max-width: 860px) {
  .shell {
    grid-template-columns: 64px 1fr;
  }
}
</style>
