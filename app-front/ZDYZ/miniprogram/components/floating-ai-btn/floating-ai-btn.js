Component({
  methods: {
    goToChat() {
      wx.navigateTo({
        url: '/pages/aiChat/aiChat',
        fail: (err) => {
            console.error('Navigation failed', err);
            // Fallback if not registered yet or path error
            wx.showToast({
                title: '无法打开聊天',
                icon: 'none'
            });
        }
      });
    }
  }
});
