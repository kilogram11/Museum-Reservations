import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import VenueForm from '@/components/VenueForm.vue'
import ElementPlus from 'element-plus'

vi.mock('@/components/BaiduMapPicker.vue', () => ({
  default: {
    template: '<div />'
  }
}))

describe('VenueForm.vue 表单验证测试', () => {
  let wrapper

  const defaultFormData = {
    id: '',
    museumTitle: '',
    museumDesc: '',
    museumAddress: '',
    museumPhone: '',
    museumBookSet: 7,
    times: [{ time: '08:00-10:00', num: 500 }],
    museumCover: '',
    startDate: '',
    endDate: '',
    museumContent: '',
    museumStatus: 0,
    museumImgs: [],
    longitude: 0,
    latitude: 0
  }

  beforeEach(() => {
    vi.clearAllMocks()
    wrapper = mount(VenueForm, {
      global: {
        plugins: [ElementPlus]
      },
      props: {
        formData: defaultFormData,
        isEdit: false
      }
    })
  })

  // 等价类划分：场馆名称验证
  describe('场馆名称字段验证', () => {
    it('应该接受有效的场馆名称', async () => {
      wrapper.setProps({
        formData: {
          ...defaultFormData,
          museumTitle: '测试场馆',
          startDate: '2024-01-01',
          endDate: '2024-12-31'
        }
      })
      await wrapper.vm.$nextTick()
      
      const form = wrapper.findComponent({ ref: 'venueRef' })
      const result = await form.vm.validate()
      expect(result).toBe(true)
    })
  })

  // 等价类划分：开放日期验证
  describe('开放日期字段验证', () => {
    it('应该接受完整的日期范围', async () => {
      wrapper.setProps({
        formData: {
          ...defaultFormData,
          museumTitle: '测试场馆',
          startDate: '2024-01-01',
          endDate: '2024-12-31'
        }
      })
      await wrapper.vm.$nextTick()
      
      const form = wrapper.findComponent({ ref: 'venueRef' })
      const result = await form.vm.validate()
      expect(result).toBe(true)
    })
  })

  // 等价类划分：联系电话验证
  describe('联系电话字段验证', () => {
    it('应该接受有效的手机号码', async () => {
      wrapper.setProps({
        formData: {
          ...defaultFormData,
          museumTitle: '测试场馆',
          museumPhone: '13800138000',
          startDate: '2024-01-01',
          endDate: '2024-12-31'
        }
      })
      await wrapper.vm.$nextTick()
      
      const form = wrapper.findComponent({ ref: 'venueRef' })
      const result = await form.vm.validate()
      expect(result).toBe(true)
    })

    it('应该接受空的联系电话（非必填）', async () => {
      wrapper.setProps({
        formData: {
          ...defaultFormData,
          museumTitle: '测试场馆',
          museumPhone: '',
          startDate: '2024-01-01',
          endDate: '2024-12-31'
        }
      })
      await wrapper.vm.$nextTick()
      
      const form = wrapper.findComponent({ ref: 'venueRef' })
      const result = await form.vm.validate()
      expect(result).toBe(true)
    })
  })

  // 等价类划分：可预约提前天数验证
  describe('可预约提前天数字段验证', () => {
    it('应该接受有效的天数（大于0）', async () => {
      wrapper.setProps({
        formData: {
          ...defaultFormData,
          museumTitle: '测试场馆',
          museumBookSet: 7,
          startDate: '2024-01-01',
          endDate: '2024-12-31'
        }
      })
      await wrapper.vm.$nextTick()
      
      const form = wrapper.findComponent({ ref: 'venueRef' })
      const result = await form.vm.validate()
      expect(result).toBe(true)
    })
  })
})