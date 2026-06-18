const { post } = require('./utils/request');

App({
  globalData: {
    isLogin: false, // 登录状态
    userInfo: null  // 用户信息（登录后存储）
  },
  onLaunch() {
    // 检查本地是否有Token，没有则引导登录
    const token = wx.getStorageSync('token');
    if (!token) {
      this.login();
    }
  },
  
  // 手机号登录（实际项目需结合微信手机号授权）
  login(mobile = '13812345678') { // 测试用手机号
    post('/app/user/login', {
      mobile: mobile,
      code: '1234' // 测试固定验证码
    }).then(data => {
      // 保存Token到本地
      wx.setStorageSync('token', data.token);
      console.log('登录成功，Token已保存');
    }).catch(err => {
      console.error('登录失败', err);
    });
  },

  // 更新未读消息气泡 - 全局通用
  updateUnreadBadge() {
    const userInfo = this.globalData.userInfo || wx.getStorageSync('userInfo');
    if (!userInfo || !userInfo.userId) {
      wx.removeTabBarBadge({ index: 2 }); // 移除消息Tab(index=2)的气泡
      return; 
    }

    wx.request({
      url: 'http://localhost:8081/message/unread/count',
      data: { userId: userInfo.userId },
      success: (res) => {
        if (res.data.code === 200) {
          const count = res.data.data;
          
          // 1. 设置底部 TabBar 气泡
          if (count > 0) {
            wx.setTabBarBadge({
              index: 2, // 消息页面在 list 中的索引是 2
              text: String(count)
            });
          } else {
            wx.removeTabBarBadge({ index: 2 });
          }

          // 2. 保存到全局，供 Profile 页面使用
          this.globalData.unreadCount = count;
          // 如果 Profile 页面已加载，可能需要通知它更新 (这里简化，让Profile的onShow自己去读或调)
        }
      }
    });
  }
});
// App({
//   globalData: {
//     isLogin: false, // 登录状态
//     userInfo: null  // 用户信息（登录后存储）
//   },
//   onLaunch() {
//     // 启动时读取本地存储的登录状态
//     const loginStatus = wx.getStorageSync('isLogin');
//     const userInfo = wx.getStorageSync('userInfo');
//     if (loginStatus) {
//       this.globalData.isLogin = loginStatus;
//       this.globalData.userInfo = userInfo;
//     }
//   }
// })