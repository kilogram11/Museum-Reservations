// pages/notice/notice.js
import { getNewsList } from '../../api/news';

Page({
  data: {
    noticeList: [],
    loading: false,
    page: 1,
    hasMore: true
  },

  onLoad(options) {
    this.fetchNotices(true);
  },

  async fetchNotices(isRefresh = false) {
    if (this.data.loading) return;
    if (!isRefresh && !this.data.hasMore) return;

    try {
      this.setData({ loading: true });
      const page = isRefresh ? 1 : this.data.page;
      const res = await getNewsList({
        page: page,
        limit: 10
      });

      if (res.code === 200) {
        const records = res.data.records || [];
        const formattedRecords = records.map(item => ({
          id: item.id,
          title: item.newsTitle,
          summary: item.newsSummary || (item.newsDesc ? item.newsDesc.replace(/<[^>]+>/g, '').substring(0, 100) : ''),
          date: item.newsAddTime ? new Date(item.newsAddTime).toLocaleDateString() : ''
        }));

        this.setData({
          noticeList: isRefresh ? formattedRecords : this.data.noticeList.concat(formattedRecords),
          page: page + 1,
          hasMore: records.length === 10
        });
      }
    } catch (error) {
      console.error('获取公告失败:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/noticeDetail/noticeDetail?id=${id}`,
    });
  },

  onPullDownRefresh() {
    this.fetchNotices(true).then(() => {
      wx.stopPullDownRefresh();
    });
  },

  onReachBottom() {
    this.fetchNotices();
  }
});