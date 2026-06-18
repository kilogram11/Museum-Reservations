import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import VenueManage from '@/views/VenueManage.vue'
import ElementPlus from 'element-plus'
import { venueApi } from '@/api/venue'

vi.mock('@/api/venue', () => ({
  venueApi: {
    list: vi.fn(),
    detail: vi.fn(),
    add: vi.fn(),
    edit: vi.fn(),
    delete: vi.fn(),
    status: vi.fn()
  }
}))

vi.mock('@/components/VenueForm.vue', () => ({
  default: {
    template: '<div class="venue-form" />',
    props: ['formData', 'isEdit']
  }
}))

vi.mock('@/components/VenuePreview.vue', () => ({
  default: {
    template: '<div class="venue-preview" />',
    props: ['visible', 'previewData']
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    back: vi.fn()
  })
}))

describe('VenueManage.vue 核心逻辑测试', () => {
  let wrapper

  const mockVenueList = [
    {
      id: 1,
      museumTitle: '测试场馆1',
      museumObj: JSON.stringify({ desc: '场馆简介1', address: '地址1', phone: '13800138001', times: [{ start: '08:00', end: '10:00', limit: 500 }] }),
      museumStatus: 1,
      museumBookSet: 7,
      museumCover: '',
      latitude: 116.40,
      longitude: 39.90
    },
    {
      id: 2,
      museumTitle: '测试场馆2',
      museumObj: JSON.stringify({ desc: '场馆简介2', address: '地址2', phone: '13800138002', times: [] }),
      museumStatus: 0,
      museumBookSet: 3,
      museumCover: '',
      latitude: 116.41,
      longitude: 39.91
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    
    venueApi.list.mockResolvedValue({
      code: 200,
      data: {
        records: mockVenueList,
        total: 2
      }
    })

    wrapper = mount(VenueManage, {
      global: {
        plugins: [ElementPlus]
      }
    })
  })

  // 场景法：场馆列表加载
  describe('场馆列表加载', () => {
    it('应该在挂载时调用API获取场馆列表', async () => {
      await wrapper.vm.$nextTick()
      expect(venueApi.list).toHaveBeenCalledWith({ page: 1, limit: 10 })
    })

    it('应该正确渲染场馆列表', async () => {
      await wrapper.vm.$nextTick()
      const rows = wrapper.findAll('tbody tr')
      expect(rows.length).toBe(2)
    })

    it('应该正确显示场馆名称', async () => {
      await wrapper.vm.$nextTick()
      const nameCells = wrapper.findAll('tbody tr td:first-child')
      expect(nameCells[0].text()).toBe('测试场馆1')
      expect(nameCells[1].text()).toBe('测试场馆2')
    })
  })

  // 场景法：新增场馆
  describe('新增场馆', () => {
    it('应该在点击新建按钮后显示表单', async () => {
      await wrapper.vm.$nextTick()
      const addBtn = wrapper.find('.card-header .el-button--primary')
      await addBtn.trigger('click')
      
      expect(wrapper.vm.isEdit).toBe(true)
      expect(wrapper.find('.venue-form').exists()).toBe(true)
    })

    it('应该初始化表单为默认值', async () => {
      await wrapper.vm.$nextTick()
      const addBtn = wrapper.find('.card-header .el-button--primary')
      await addBtn.trigger('click')
      
      const form = wrapper.vm.venueForm
      expect(form.id).toBe('')
      expect(form.museumTitle).toBe('')
      expect(form.museumBookSet).toBe(7)
      expect(form.times.length).toBe(1)
    })
  })

  // 场景法：编辑场馆
  describe('编辑场馆', () => {
    it('应该在点击编辑按钮后显示表单', async () => {
      await wrapper.vm.$nextTick()
      const editBtn = wrapper.findAll('.el-button--primary').at(1)
      await editBtn.trigger('click')
      
      expect(wrapper.vm.isEdit).toBe(true)
      expect(wrapper.find('.venue-form').exists()).toBe(true)
    })

    it('应该正确加载场馆数据到表单', async () => {
      await wrapper.vm.$nextTick()
      const editBtn = wrapper.findAll('.el-button--primary').at(1)
      await editBtn.trigger('click')
      
      const form = wrapper.vm.venueForm
      expect(form.id).toBe(1)
      expect(form.museumTitle).toBe('测试场馆1')
      expect(form.museumBookSet).toBe(7)
      expect(form.museumStatus).toBe(1)
    })
  })
})