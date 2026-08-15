import { reactive } from 'vue'

const STORAGE_KEY = 'xp_user'

function load() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) || {}
  } catch {
    return {}
  }
}

const saved = load()

export const user = reactive({
  id: saved.id ?? null,
  nickName: saved.nickName ?? '',
  phone: saved.phone ?? '',
  loginDialogOpen: false
})

export function setUser(u) {
  user.id = u.id
  user.nickName = u.nickName
  user.phone = u.phone
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ id: u.id, nickName: u.nickName, phone: u.phone }))
}

export function logout() {
  user.id = null
  user.nickName = ''
  user.phone = ''
  localStorage.removeItem(STORAGE_KEY)
}

export function requireLogin() {
  if (user.id) return true
  user.loginDialogOpen = true
  return false
}
