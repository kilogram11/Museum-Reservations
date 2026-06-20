/**
 * 测试环境 setup - 模拟微信小程序运行时环境
 */

// 模拟全局 wx 对象
global.wx = {
  // Storage
  getStorageSync: jest.fn((key) => {
    const store = global.__storage || {};
    return store[key];
  }),
  setStorageSync: jest.fn((key, value) => {
    if (!global.__storage) global.__storage = {};
    global.__storage[key] = value;
  }),
  removeStorageSync: jest.fn((key) => {
    if (global.__storage) delete global.__storage[key];
  }),
  clearStorageSync: jest.fn(() => {
    global.__storage = {};
  }),

  // UI
  showToast: jest.fn(),
  showLoading: jest.fn(),
  hideLoading: jest.fn(),
  showModal: jest.fn(),
  showActionSheet: jest.fn(),

  // Navigation
  navigateTo: jest.fn(),
  navigateBack: jest.fn(),
  switchTab: jest.fn(),
  reLaunch: jest.fn(),
  redirectTo: jest.fn(),

  // Tab bar
  setTabBarBadge: jest.fn(),
  removeTabBarBadge: jest.fn(),

  // Network
  request: jest.fn(),
  uploadFile: jest.fn(),

  // System
  getSystemInfoSync: jest.fn(() => ({
    SDKVersion: '2.20.0',
    platform: 'windows',
    brand: 'devtools',
    model: 'PC',
    system: 'Windows',
    pixelRatio: 2,
    screenWidth: 1920,
    screenHeight: 1080,
    windowWidth: 1920,
    windowHeight: 1080,
    language: 'zh_CN',
    version: '8.0.0'
  })),
  getAccountInfoSync: jest.fn(() => ({
    miniProgram: {
      envVersion: 'develop',
      appId: 'wx-test-app-id'
    }
  })),

  // Media
  chooseMedia: jest.fn(),
  previewImage: jest.fn(),
  openLocation: jest.fn(),

  // Other
  canIUse: jest.fn(() => true),
  getUpdateManager: jest.fn(() => ({
    onCheckForUpdate: jest.fn(),
    onUpdateReady: jest.fn(),
    onUpdateFailed: jest.fn(),
    applyUpdate: jest.fn()
  })),
  createSelectorQuery: jest.fn(() => ({
    select: jest.fn(() => ({
      boundingClientRect: jest.fn(() => ({
        exec: jest.fn()
      })),
      scrollOffset: jest.fn(() => ({
        exec: jest.fn()
      }))
    })),
    selectAll: jest.fn(() => ({
      boundingClientRect: jest.fn(() => ({
        exec: jest.fn()
      }))
    })),
    exec: jest.fn()
  })),
  createAnimation: jest.fn(() => ({
    opacity: jest.fn().mockReturnThis(),
    translate: jest.fn().mockReturnThis(),
    scale: jest.fn().mockReturnThis(),
    step: jest.fn().mockReturnThis(),
    export: jest.fn()
  })),

  // Cloud
  cloud: {
    init: jest.fn(),
    callFunction: jest.fn()
  },

  // Login
  login: jest.fn(),
  checkSession: jest.fn(),

  // Payment
  requestPayment: jest.fn(),

  // Scan
  scanCode: jest.fn(),

  // Clipboard
  setClipboardData: jest.fn(),
  getClipboardData: jest.fn(),

  // File
  getFileSystemManager: jest.fn(() => ({
    readFile: jest.fn(),
    writeFile: jest.fn(),
    access: jest.fn(),
    mkdir: jest.fn(),
    saveFile: jest.fn()
  })),

  // Network info
  getNetworkType: jest.fn(),
  onNetworkStatusChange: jest.fn(),

  // Location
  getLocation: jest.fn(),
  chooseLocation: jest.fn()
};

// 模拟 Page 构造函数
global.Page = jest.fn((config) => {
  // 返回配置，以便测试中可以访问
  return config;
});

// 模拟 App 构造函数
global.App = jest.fn((config) => {
  global.__appConfig = config;
  return config;
});

// 模拟 Component 构造函数
global.Component = jest.fn((config) => {
  return config;
});

// 模拟 getApp
global.getApp = jest.fn(() => {
  return global.__appConfig || {
    globalData: {
      isLogin: false,
      userInfo: null,
      unreadCount: 0
    },
    updateUnreadBadge: jest.fn(),
    login: jest.fn()
  };
});

// 模拟 Promise
global.setTimeout = setTimeout;
global.clearTimeout = clearTimeout;
global.setInterval = setInterval;
global.clearInterval = clearInterval;

// 模拟 console
global.console = {
  log: jest.fn(),
  error: jest.fn(),
  warn: jest.fn(),
  info: jest.fn(),
  debug: jest.fn()
};

// 模拟 Math
global.Math = Math;

// 模拟 encodeURIComponent / decodeURIComponent
global.encodeURIComponent = encodeURIComponent;
global.decodeURIComponent = decodeURIComponent;

// 模拟 Date
global.Date = Date;

// 模拟 JSON
global.JSON = JSON;