// pages/profile/profile.js
import { getUserInfo } from '../../api/user';
const app = getApp();

Page({
  data: {
    isLogin: false,
    userInfo: null,
    loading: false,
    unreadCount: 0
  },

  onShow() {
    this.checkLoginStatus();

    // 如果是从预约成功跳转过来的，自动打开预约记录，且让返回键回到 Profile
    if (wx.getStorageSync('fromReserveSuccess')) {
      wx.removeStorageSync('fromReserveSuccess');
      wx.navigateTo({ url: '/pages/orderRecord/orderRecord' });
    }

    // 每次显示时更新气泡
    if (typeof app.updateUnreadBadge === 'function') {
      app.updateUnreadBadge();
    }
    // 同步 globalData (因为app.js的回调可能异步，这里先读一下旧的，或者更好的方式是app.js里回调通知，这里简单轮询即可)
    this.setData({
      unreadCount: app.globalData.unreadCount || 0
    });
    // 监听 globalData 变化比较麻烦，这里利用 JS 单线程特性，app.updateUnreadBadge 里的 success 是异步的
    // 所以由于 onShow 频繁触发，这里其实依赖下一次 onShow 或者 app.js 修改 globalData 后再次 setData
    // 更好的做法是：
    const that = this;
    // 覆盖 app.js 的 success 回调? No.
    // 我们手动调 API 也不错，但为了复用 app.js 逻辑.
    // Hack: 几百毫秒后读一下?
    setTimeout(() => {
      that.setData({ unreadCount: app.globalData.unreadCount || 0 });
    }, 500);
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token');
    if (token) {
      this.setData({ isLogin: true });
      this.fetchUserInfo();
    } else {
      this.setData({ isLogin: false, userInfo: null });
    }
  },

  async fetchUserInfo() {
    try {
      this.setData({ loading: true });
      const res = await getUserInfo();
      if (res.code === 200) {
        const data = res.data;
        const formatted = {
          name: data.userName || '博物馆用户',
          phone: data.userMobile,
          userId: data.userId,
          userPic: data.userPic,
          // 优先使用后端关联返回的 URL，否则根据 ID 拼接，再无则默认 1.png
          avatarUrl: data.userPicUrl || (data.userPic ? `/images/avatars/${data.userPic}.png` : '/images/avatars/1.png')
        };
        this.setData({ userInfo: formatted });
        wx.setStorageSync('userInfo', formatted);
      }
    } catch (e) {
      console.error('获取信息失败:', e);
    } finally {
      this.setData({ loading: false });
    }
  },

  goToLogin() { wx.navigateTo({ url: '/pages/login/login' }); },
  goToOrderRecord(e) {
    const tab = e.currentTarget.dataset.tab || 0;
    wx.navigateTo({ url: `/pages/orderRecord/orderRecord?tab=${tab}` });
  },
  goToVisitorReg() { wx.navigateTo({ url: '/pages/visitorRegistration/visitorRegistration' }); },
  goToEditProfile() { wx.navigateTo({ url: '/pages/editProfile/editProfile' }); },
  goToPrivacy() { wx.navigateTo({ url: '/pages/privacy/privacy' }); },
  goToFaq() { wx.navigateTo({ url: '/pages/faq/faq' }); },
  goToMessage() { wx.switchTab({ url: '/pages/message/message' }); },

  logout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.clearStorageSync();
          this.setData({ isLogin: false, userInfo: null });
          wx.showToast({
            title: '已退出',
            icon: 'success',
            complete: () => {
              setTimeout(() => {
                wx.reLaunch({ url: '/pages/welcome/welcome' });
              }, 1000);
            }
          });
        }
      }
    });
  }
});