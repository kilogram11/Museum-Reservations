Page({
  data: {
    countdown: 5,
    timer: null
  },

  onLoad() {
    this.startCountdown();
  },

  onUnload() {
    if (this.data.timer) {
      clearInterval(this.data.timer);
    }
  },

  startCountdown() {
    let count = 5;
    const timer = setInterval(() => {
      count--;
      if (count <= 0) {
        clearInterval(timer);
        this.setData({
          countdown: 0,
          timer: null
        });
      } else {
        this.setData({
          countdown: count
        });
      }
    }, 1000);
    this.setData({ timer });
  },

  confirmNotice() {
    if (this.data.countdown > 0) return;
    wx.navigateTo({
      url: '/pages/reserve/reserve'
    });
  }
});
