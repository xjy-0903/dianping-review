<script setup>
import { reactive, ref } from 'vue'
import { api } from '../api'
import { user, setUser } from '../store/user'
import { toast } from '../utils/toast'

const form = reactive({ phone: '', password: '' })
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!/^1\d{10}$/.test(form.phone)) {
    error.value = '请输入 11 位手机号'
    return
  }
  if (!form.password) {
    error.value = '请输入密码'
    return
  }
  loading.value = true
  try {
    const u = await api.login({ phone: form.phone, password: form.password })
    setUser(u)
    user.loginDialogOpen = false
    toast(`欢迎回来,${u.nickName}`, 'success')
    form.password = ''
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <teleport to="body">
    <transition name="fade">
      <div v-if="user.loginDialogOpen" class="mask" @click.self="user.loginDialogOpen = false">
        <div class="dialog card rise">
          <div class="dialog-head">
            <div>
              <h3>登录巷评</h3>
              <p>演示账号:13800138000 ~ 2,密码 123456,新手机号自动注册</p>
            </div>
            <button class="close" aria-label="关闭" @click="user.loginDialogOpen = false">✕</button>
          </div>
          <div class="field">
            <label>手机号</label>
            <input v-model="form.phone" placeholder="13800138000" inputmode="numeric" maxlength="11" @keyup.enter="submit" />
          </div>
          <div class="field">
            <label>密码</label>
            <input v-model="form.password" type="password" placeholder="123456" @keyup.enter="submit" />
          </div>
          <p v-if="error" class="error">{{ error }}</p>
          <button class="btn btn-primary login-btn" :disabled="loading" @click="submit">
            {{ loading ? '登录中…' : '登录 / 注册' }}
          </button>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(20, 26, 23, 0.5);
  display: grid;
  place-items: center;
  z-index: 90;
}

.dialog {
  width: min(400px, calc(100vw - 40px));
  padding: 26px;
}

.dialog-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.dialog-head h3 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 800;
}

.dialog-head p {
  margin: 0;
  font-size: 12px;
  color: var(--ink-faint);
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

.dialog .field + .field {
  margin-top: 14px;
}

.error {
  color: var(--vermilion);
  font-size: 13px;
  margin: 12px 0 0;
}

.login-btn {
  width: 100%;
  margin-top: 18px;
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
