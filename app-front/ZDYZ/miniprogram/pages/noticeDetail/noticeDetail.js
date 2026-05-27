// pages/noticeDetail/noticeDetail.js
import { getNewsDetail } from '../../api/news';

Page({
  data: {
    detail: {},
    loading: false
  },

  onLoad(options) {
    const id = options.id;
    if (id) {
      this.fetchDetail(id);
    }
  },

  async fetchDetail(id) {
    try {
      this.setData({ loading: true });
      const res = await getNewsDetail(id);
      if (res.code === 200) {
        const item = res.data;
        this.setData({
          detail: {
            title: item.newsTitle,
            content: item.newsDesc,
            date: item.newsAddTime ? new Date(item.newsAddTime).toLocaleDateString() : '',
            author: '博物馆管理员',
            category: '官方公告'
          }
        });
      }
    } catch (error) {
      console.error('获取公告详情失败:', error);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  }
});