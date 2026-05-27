// pages/relic3d/relic3d.js
import { baseUrl } from '../../utils/request';

Page({
  data: {
    url: ''
  },
  onLoad(options) {
    if (options.modelUrl) {
      // 1. 解码参数（防止双重编码问题）
      let rawModelPath = decodeURIComponent(options.modelUrl);
      // 如果解码后还包含 %2F，说明可能是多重编码，再次解码直到正常
      while (rawModelPath.includes('%2F') || rawModelPath.includes('%2f')) {
        rawModelPath = decodeURIComponent(rawModelPath);
      }

      // 2. 构造 BaseURL
      // 尝试将 localhost 替换为 127.0.0.1 以规避部分环境对 localhost 的特殊限制
      let finalBaseUrl = baseUrl;
      if (finalBaseUrl.includes('localhost')) {
        finalBaseUrl = finalBaseUrl.replace('localhost', '127.0.0.1');
      }

      // 3. 构造最终 WebView URL (重命名为 relic_preview.html 以绕过任何残留缓存)
      const name = options.name || '';
      const desc = options.desc || '';
      const viewerUrl = `${finalBaseUrl}/relic_preview.html?model=${encodeURIComponent(rawModelPath)}&name=${encodeURIComponent(name)}&desc=${encodeURIComponent(desc)}&t=${Date.now()}`;

      console.log('3D Viewer Target URL:', viewerUrl);
      this.setData({ url: viewerUrl });
    }
  },

  handleLoad(e) {
    console.log('WebView Loaded Success:', e);
    wx.showToast({ title: '加载成功', icon: 'success' });
  },

  handleError(e) {
    console.error('WebView Load Error:', e);
    wx.showModal({
      title: '3D页面加载失败',
      content: '请检查：\n1. 开发者工具右上角详情-本地设置-勾选"不校验合法域名"\n2. 确保后端服务已启动\n3. 错误详情: ' + JSON.stringify(e.detail),
      showCancel: false
    });
  }
});
