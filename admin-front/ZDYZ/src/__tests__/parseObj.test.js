import { describe, it, expect } from 'vitest'

const parseObj = (jsonStr) => {
  try {
    return jsonStr ? JSON.parse(jsonStr) : {}
  } catch (e) {
    console.error('JSON Parse Error:', e)
    return {}
  }
}

describe('parseObj 工具函数测试', () => {
  // 等价类划分：有效等价类测试
  it('应该正确解析有效的JSON字符串', () => {
    const jsonStr = '{"name":"场馆A","desc":"测试场馆"}'
    const result = parseObj(jsonStr)
    expect(result).toEqual({ name: '场馆A', desc: '测试场馆' })
  })

  // 边界值分析：空值边界测试
  it('应该处理空字符串返回空对象', () => {
    const result = parseObj('')
    expect(result).toEqual({})
  })

  // 边界值分析：null边界测试
  it('应该处理null返回空对象', () => {
    const result = parseObj(null)
    expect(result).toEqual({})
  })

  // 等价类划分：无效等价类测试
  it('应该处理无效JSON字符串返回空对象', () => {
    const invalidJson = '{"name":"场馆A", desc:"测试场馆"}'
    const result = parseObj(invalidJson)
    expect(result).toEqual({})
  })

  // 等价类划分：复杂嵌套JSON测试
  it('应该正确解析嵌套的JSON对象', () => {
    const jsonStr = '{"level1":{"level2":{"value":123}}}'
    const result = parseObj(jsonStr)
    expect(result.level1.level2.value).toBe(123)
  })
})