import { describe, it, expect } from 'vitest'
import {
  RESPONSE_CODE,
  STATUS,
  COLORS,
  DATE_FORMAT,
  PAGINATION,
  STORAGE_KEY,
  API_PATH
} from '@/config/constants'

describe('constants.js 常量配置测试', () => {
  // 路径覆盖：测试 RESPONSE_CODE 常量对象
  describe('RESPONSE_CODE 响应状态码', () => {
    it('应该导出正确的状态码数值', () => {
      expect(RESPONSE_CODE.SUCCESS).toBe(200)
      expect(RESPONSE_CODE.ERROR).toBe(500)
      expect(RESPONSE_CODE.UNAUTHORIZED).toBe(401)
      expect(RESPONSE_CODE.FORBIDDEN).toBe(403)
      expect(RESPONSE_CODE.NOT_FOUND).toBe(404)
    })

    it('状态码应该是数字类型', () => {
      expect(typeof RESPONSE_CODE.SUCCESS).toBe('number')
      expect(typeof RESPONSE_CODE.ERROR).toBe('number')
    })
  })

  // 路径覆盖：测试 STATUS 常量对象
  describe('STATUS 状态枚举值', () => {
    it('应该导出正确的状态枚举值', () => {
      expect(STATUS.DISABLED).toBe(0)
      expect(STATUS.ENABLED).toBe(1)
      expect(STATUS.CANCELLED).toBe(2)
      expect(STATUS.CHECKED_IN).toBe(1)
      expect(STATUS.UNCHECKED).toBe(0)
      expect(STATUS.NO_SHOW).toBe(3)
      expect(STATUS.PENDING).toBe(0)
    })

    it('状态值应该是数字类型', () => {
      expect(typeof STATUS.DISABLED).toBe('number')
      expect(typeof STATUS.ENABLED).toBe('number')
    })
  })

  // 路径覆盖：测试 COLORS 常量对象
  describe('COLORS 颜色主题', () => {
    it('应该导出正确的颜色值', () => {
      expect(COLORS.PRIMARY).toBe('#ACF44A')
      expect(COLORS.PRIMARY_DARK).toBe('#0B2118')
      expect(COLORS.SUCCESS).toBe('#00B111')
      expect(COLORS.WARNING).toBe('#FAC858')
      expect(COLORS.DANGER).toBe('#FF5E5E')
      expect(COLORS.INFO).toBe('#5470c6')
      expect(COLORS.TEXT_PRIMARY).toBe('#1A1D1F')
      expect(COLORS.TEXT_SECONDARY).toBe('#6F767E')
      expect(COLORS.BACKGROUND).toBe('#F7F8FA')
      expect(COLORS.BORDER).toBe('#F4F4F4')
    })

    it('颜色值应该是字符串类型且以#开头', () => {
      expect(typeof COLORS.PRIMARY).toBe('string')
      expect(COLORS.PRIMARY.startsWith('#')).toBe(true)
    })
  })

  // 路径覆盖：测试 DATE_FORMAT 常量对象
  describe('DATE_FORMAT 日期格式', () => {
    it('应该导出正确的日期格式字符串', () => {
      expect(DATE_FORMAT.YYYY_MM_DD).toBe('YYYY-MM-DD')
      expect(DATE_FORMAT.YYYY_MM_DD_HH_MM).toBe('YYYY-MM-DD HH:mm')
      expect(DATE_FORMAT.YYYY_MM_DD_HH_MM_SS).toBe('YYYY-MM-DD HH:mm:ss')
      expect(DATE_FORMAT.DISPLAY).toBe('YYYY年MM月DD日 HH:mm:ss')
      expect(DATE_FORMAT.DISPLAY_SHORT).toBe('YYYY年MM月DD日')
    })

    it('日期格式应该是字符串类型', () => {
      expect(typeof DATE_FORMAT.YYYY_MM_DD).toBe('string')
    })
  })

  // 路径覆盖：测试 PAGINATION 常量对象
  describe('PAGINATION 分页配置', () => {
    it('应该导出正确的分页默认值', () => {
      expect(PAGINATION.DEFAULT_PAGE).toBe(1)
      expect(PAGINATION.DEFAULT_LIMIT).toBe(10)
      expect(PAGINATION.PAGE_SIZES).toEqual([5, 10, 20, 50, 100])
    })

    it('分页配置应该是正确的类型', () => {
      expect(typeof PAGINATION.DEFAULT_PAGE).toBe('number')
      expect(Array.isArray(PAGINATION.PAGE_SIZES)).toBe(true)
    })
  })

  // 路径覆盖：测试 STORAGE_KEY 常量对象
  describe('STORAGE_KEY 存储键名', () => {
    it('应该导出正确的存储键名', () => {
      expect(STORAGE_KEY.TOKEN).toBe('token')
      expect(STORAGE_KEY.ADMIN_ID).toBe('adminId')
      expect(STORAGE_KEY.USER_AVATAR).toBe('userAvatar')
    })

    it('存储键名应该是字符串类型', () => {
      expect(typeof STORAGE_KEY.TOKEN).toBe('string')
    })
  })

  // 路径覆盖：测试 API_PATH 常量对象
  describe('API_PATH API路径前缀', () => {
    it('应该导出正确的API路径前缀', () => {
      expect(API_PATH.AUTH).toBe('/admin/auth')
      expect(API_PATH.MUSEUM).toBe('/admin/museum')
      expect(API_PATH.ACTIVITY).toBe('/admin/activity')
      expect(API_PATH.JOIN).toBe('/admin/join')
      expect(API_PATH.NEWS).toBe('/admin/news')
      expect(API_PATH.BLACKLIST).toBe('/admin/blacklist')
      expect(API_PATH.MESSAGE).toBe('/admin/message')
      expect(API_PATH.STATS).toBe('/stats')
    })

    it('API路径应该是字符串类型且以斜杠开头', () => {
      expect(typeof API_PATH.AUTH).toBe('string')
      expect(API_PATH.AUTH.startsWith('/')).toBe(true)
    })
  })
})