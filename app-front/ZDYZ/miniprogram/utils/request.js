// utils/request.js
export const baseUrl = 'http://127.0.0.1:8081';

// 统一的响应处理
function handleResponse(res) {
  console.log('响应状态码:', res.statusCode);
  console.log('响应数据:', res.data);

  // 检查HTTP状态码
  if (res.statusCode === 200) {
    // 检查响应数据是否有效
    if (res.data !== undefined && res.data !== null) {
      return res.data;
    } else {
      throw new Error('响应数据为空');
    }
  } else if (res.statusCode === 401) {
    throw new Error('登录已过期，请重新登录');
  } else if (res.statusCode === 403) {
    throw new Error('无权限访问');
  } else {
    throw new Error(`HTTP错误: ${res.statusCode}`);
  }
}

export const get = (url, data = null, options = {}) => {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const header = { ...options.header };
    if (token) {
      header['Token'] = token;
    }

    const fullUrl = baseUrl + url;
    console.log('--- [Request] GET ---');
    console.log('Path:', url);

    wx.request({
      url: fullUrl,
      method: 'GET',
      header: header,
      // 彻底不传 data 字段，防止底层转换方法
      success: (res) => {
        try {
          const result = handleResponse(res);
          resolve(result);
        } catch (error) {
          reject(error);
        }
      },
      fail: (error) => {
        console.error(`GET ${url} Fail:`, error);
        reject(new Error('网络请求失败'));
      }
    });
  });
};

export const post = (url, data = {}, options = {}) => {
  return new Promise((resolve, reject) => {
    // 从本地存储获取token
    const token = wx.getStorageSync('token');

    // 设置请求头
    const header = {
      'content-type': 'application/json',
      ...options.header
    };

    // 如果有token，添加到请求头
    if (token) {
      header['Token'] = token;
    }

    // 完整的URL
    const fullUrl = baseUrl + url;
    console.log('发送POST请求:', fullUrl);
    console.log('请求数据:', data);

    wx.request({
      url: fullUrl,
      method: 'POST',
      data: data,
      header: header,
      success: (res) => {
        try {
          const data = handleResponse(res);
          resolve(data);
        } catch (error) {
          reject(error);
        }
      },
      fail: (error) => {
        console.error(`POST ${url} 请求失败:`, error);
        reject(new Error('网络请求失败，请检查网络连接'));
      }
    });
  });
};

// 导出对象以兼容旧的 require 方式
module.exports = {
  get,
  post,
  baseUrl
};
