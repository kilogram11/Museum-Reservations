const app = getApp();

Page({
  data: {
    id: '',
    title: '',
    content: '',
    createTime: '',
    type: 0 // 1: booking, 2: activity
  },

  onLoad(options) {
    if (options.id) {
      this.setData({
        id: options.id,
        title: decodeURIComponent(options.title || ''),
        content: decodeURIComponent(options.content || ''),
        createTime: options.createTime || '',
        type: parseInt(options.type || 0)
      });
      
      this.markAsRead(options.id);
    }
  },

  markAsRead(id) {
    wx.request({
      url: `http://localhost:8081/message/read/${id}`,
      method: 'POST',
      success: (res) => {
        if (res.data.code === 200) {
          console.log('Message marked as read');
          // Optionally notify previous page to update status, but simple reload on show is easier
        }
      },
      fail: (err) => {
        console.error('Failed to mark read', err);
      }
    });
  }
});
