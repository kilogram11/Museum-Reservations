const app = getApp()

Page({
  data: {
    msgList: [],
    loading: false
  },

  onShow() {
    this.getMsgList()
  },

  getMsgList() {
    this.setData({ loading: true })
    
    // Fix: Extract userId from userInfo object
    const userInfo = getApp().globalData.userInfo || wx.getStorageSync('userInfo');
    const userId = userInfo ? userInfo.userId : '';

    if (!userId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      this.setData({ loading: false });
      return;
    }
    
    wx.request({
      url: 'http://localhost:8081/message/my', 
      method: 'GET',
      data: {
          userId: userId 
      },
      success: (res) => {
        if (res.data.code === 200) {
          const list = res.data.data.map(item => {
            // 只保留日期部分 (假设格式为 "yyyy-MM-dd HH:mm:ss" 或 ISO)
            if (item.createTime && item.createTime.length > 10) {
                item.createTime = item.createTime.substring(0, 10);
            }
            return item;
          });
          this.setData({
            msgList: list
          })
        }
      },
      fail: (err) => {
        console.error("Fetch message failed", err);
      },
      complete: () => {
        this.setData({ loading: false })
      }
    })
  },

  goToDetail(e) {
    const item = e.currentTarget.dataset.item;
    const url = `/pages/messageDetail/messageDetail?id=${item.id}&title=${encodeURIComponent(item.title)}&content=${encodeURIComponent(item.content)}&createTime=${item.createTime}&type=${item.type}`;
    wx.navigateTo({
      url: url
    });
  }
})
