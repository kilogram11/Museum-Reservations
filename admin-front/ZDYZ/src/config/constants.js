// 响应状态码
export const RESPONSE_CODE = {
  SUCCESS: 200,
  ERROR: 500,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404
}

// 状态枚举值
export const STATUS = {
  DISABLED: 0,
  ENABLED: 1,
  CANCELLED: 2,
  CHECKED_IN: 1,
  UNCHECKED: 0,
  NO_SHOW: 3,
  PENDING: 0
}

// 颜色主题
export const COLORS = {
  PRIMARY: '#ACF44A',
  PRIMARY_DARK: '#0B2118',
  SUCCESS: '#00B111',
  WARNING: '#FAC858',
  DANGER: '#FF5E5E',
  INFO: '#5470c6',
  TEXT_PRIMARY: '#1A1D1F',
  TEXT_SECONDARY: '#6F767E',
  BACKGROUND: '#F7F8FA',
  BORDER: '#F4F4F4'
}

// 日期格式
export const DATE_FORMAT = {
  YYYY_MM_DD: 'YYYY-MM-DD',
  YYYY_MM_DD_HH_MM: 'YYYY-MM-DD HH:mm',
  YYYY_MM_DD_HH_MM_SS: 'YYYY-MM-DD HH:mm:ss',
  DISPLAY: 'YYYY年MM月DD日 HH:mm:ss',
  DISPLAY_SHORT: 'YYYY年MM月DD日'
}

// 分页默认值
export const PAGINATION = {
  DEFAULT_PAGE: 1,
  DEFAULT_LIMIT: 10,
  PAGE_SIZES: [5, 10, 20, 50, 100]
}

// 存储键名
export const STORAGE_KEY = {
  TOKEN: 'token',
  ADMIN_ID: 'adminId',
  USER_AVATAR: 'userAvatar'
}

// API路径前缀
export const API_PATH = {
  AUTH: '/admin/auth',
  MUSEUM: '/admin/museum',
  ACTIVITY: '/admin/activity',
  JOIN: '/admin/join',
  NEWS: '/admin/news',
  BLACKLIST: '/admin/blacklist',
  MESSAGE: '/admin/message',
  STATS: '/stats'
}