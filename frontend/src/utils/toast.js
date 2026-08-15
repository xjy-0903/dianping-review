import { reactive } from 'vue'

export const toasts = reactive([])

let seq = 0

export function toast(message, type = 'info', duration = 2600) {
  const id = ++seq
  toasts.push({ id, message, type })
  setTimeout(() => {
    const idx = toasts.findIndex((t) => t.id === id)
    if (idx > -1) toasts.splice(idx, 1)
  }, duration)
}
