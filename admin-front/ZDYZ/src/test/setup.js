import { beforeEach, vi } from 'vitest'
import ElementPlus from 'element-plus'
import { createApp } from 'vue'

beforeEach(() => {
  const app = createApp({})
  app.use(ElementPlus)
})

vi.stubGlobal('alert', vi.fn())
vi.stubGlobal('confirm', vi.fn(() => true))
vi.stubGlobal('prompt', vi.fn(() => ''))

const localStorageMock = {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
vi.stubGlobal('localStorage', localStorageMock)
