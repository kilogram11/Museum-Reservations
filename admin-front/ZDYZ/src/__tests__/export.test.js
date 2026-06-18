import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { exportData, exportPostData } from '@/utils/export'

vi.mock('axios')
vi.mock('element-plus', () => ({
  ElMessage: {
    info: vi.fn(),
    error: vi.fn()
  }
}))

describe('exportData 导出工具函数测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('localStorage', {
      getItem: vi.fn(() => 'test-token')
    })
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob://test-url')
    })
    const mockLink = {
      href: '',
      setAttribute: vi.fn(),
      click: vi.fn()
    }
    vi.stubGlobal('document', {
      createElement: vi.fn(() => mockLink),
      body: {
        appendChild: vi.fn(),
        removeChild: vi.fn()
      }
    })
  })

  // 边界值分析：正常情况
  it('应该成功导出数据', async () => {
    axios.get.mockResolvedValue({
      data: new Blob(['test data']),
      headers: {}
    })
    
    const result = await exportData('/api/export', 'test-file')
    
    expect(axios.get).toHaveBeenCalled()
    expect(ElMessage.info).toHaveBeenCalledWith('正在下载，请稍候...')
    expect(result).toBe(true)
  })

  // 边界值分析：空URL边界
  it('应该处理空URL参数', async () => {
    axios.get.mockResolvedValue({
      data: new Blob(['test data']),
      headers: {}
    })
    
    const result = await exportData('', 'test-file')
    
    expect(axios.get).toHaveBeenCalled()
    expect(result).toBe(true)
  })

  // 边界值分析：无token边界
  it('应该处理token为null的情况', async () => {
    vi.stubGlobal('localStorage', {
      getItem: vi.fn(() => null)
    })
    axios.get.mockResolvedValue({
      data: new Blob(['test data']),
      headers: {}
    })
    
    const result = await exportData('/api/export', 'test-file')
    
    expect(axios.get).toHaveBeenCalled()
    expect(result).toBe(true)
  })

  // 边界值分析：服务器返回文件名
  it('应该正确解析content-disposition中的文件名', async () => {
    axios.get.mockResolvedValue({
      data: new Blob(['test data']),
      headers: {
        'content-disposition': 'attachment; filename=exported_file.xlsx'
      }
    })
    
    await exportData('/api/export', 'default-name')
    
    expect(document.createElement).toHaveBeenCalledWith('a')
    const link = document.createElement.mock.results[0].value
    expect(link.setAttribute).toHaveBeenCalledWith('download', 'exported_file.xlsx')
  })

  // 等价类划分：网络异常测试
  it('应该处理网络请求失败', async () => {
    axios.get.mockRejectedValue(new Error('Network error'))
    
    const result = await exportData('/api/export', 'test-file')
    
    expect(ElMessage.error).toHaveBeenCalledWith('导出失败')
    expect(result).toBe(false)
  })
})

describe('exportPostData 导出工具函数测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('localStorage', {
      getItem: vi.fn(() => 'test-token')
    })
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob://test-url')
    })
    const mockLink = {
      href: '',
      setAttribute: vi.fn(),
      click: vi.fn()
    }
    vi.stubGlobal('document', {
      createElement: vi.fn(() => mockLink),
      body: {
        appendChild: vi.fn(),
        removeChild: vi.fn()
      }
    })
  })

  // 边界值分析：正常POST导出
  it('应该成功通过POST导出数据', async () => {
    axios.post.mockResolvedValue({
      data: new Blob(['test data']),
      headers: {}
    })
    
    const result = await exportPostData('/api/export', { id: 1 }, 'test-file')
    
    expect(axios.post).toHaveBeenCalled()
    expect(ElMessage.info).toHaveBeenCalledWith('正在下载，请稍候...')
    expect(result).toBe(true)
  })

  // 边界值分析：空数据边界
  it('应该处理空数据参数', async () => {
    axios.post.mockResolvedValue({
      data: new Blob(['test data']),
      headers: {}
    })
    
    const result = await exportPostData('/api/export', {}, 'test-file')
    
    expect(axios.post).toHaveBeenCalled()
    expect(result).toBe(true)
  })

  // 等价类划分：POST请求失败
  it('应该处理POST请求失败', async () => {
    axios.post.mockRejectedValue(new Error('Server error'))
    
    const result = await exportPostData('/api/export', { id: 1 }, 'test-file')
    
    expect(ElMessage.error).toHaveBeenCalledWith('导出失败')
    expect(result).toBe(false)
  })
})