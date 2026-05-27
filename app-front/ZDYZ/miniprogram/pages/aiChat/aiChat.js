Page({
  data: {
    messages: [
      { type: 'bot', content: '您好！我是您的智能助手，请问有什么可以帮您？' }
    ],
    inputValue: '',
    loading: false,
    toView: 'msg-0'
  },

  onInput(e) {
    this.setData({
      inputValue: e.detail.value
    });
  },

  sendMessage() {
    const content = this.data.inputValue.trim();
    if (!content || this.data.loading) return;

    this.addMessage('user', content);
    this.setData({ inputValue: '', loading: true });

    // Scroll to bottom
    this.scrollToBottom();

    wx.request({
      url: 'http://localhost:8081/ai/chat', // Verify if this matches your local/dev config
      method: 'POST',
      data: {
        message: content
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.reply) {
          this.addMessage('bot', res.data.reply);
        } else {
          this.addMessage('bot', '抱歉，服务暂时不可用。');
        }
      },
      fail: (err) => {
        console.error('API Error:', err);
        this.addMessage('bot', '网络连接失败，请稍后重试。');
      },
      complete: () => {
        this.setData({ loading: false });
        this.scrollToBottom();
      }
    });
  },

  addMessage(type, content) {
    const newMsg = { type, content };
    const messages = this.data.messages;
    messages.push(newMsg);
    this.setData({
      messages,
      toView: `msg-${messages.length - 1}`
    });
  },

  scrollToBottom() {
    const len = this.data.messages.length;
    this.setData({
        toView: `msg-${len - 1}`
    });
  }
});
