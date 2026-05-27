// pages/activity/activity.js
import { getActivityList } from '../../api/activity';
import { baseUrl } from '../../utils/request';

Page({
  data: {
    currentSwiperIndex: 0,
    swiperList: [],
    activityList: [],
    loading: false,
    page: 1,
    hasMore: true
  },

  onLoad(options) {
    this.fetchActivityData(true);
  },

  async fetchActivityData(isRefresh = false) {
    if (this.data.loading) return;
    if (!isRefresh && !this.data.hasMore) return;

    try {
      this.setData({ loading: true });
      const page = isRefresh ? 1 : this.data.page;
      const res = await getActivityList({
        page: page,
        limit: 10
      });

      if (res.code === 200) {
        const records = res.data.records || [];
        const formattedRecords = records.map(item => {
          // 解析内容 JSON 以获取缩略图
          let thumbnails = [];
          try {
            if (item.activityObj) {
              const obj = JSON.parse(item.activityObj);
              const content = obj.content || [];
              thumbnails = content.filter(c => c.type === 'img' || c.type === 'image')
                                  .map(c => (c.val && c.val.startsWith('/')) ? baseUrl + c.val : c.val)
                                  .slice(0, 4);
            }
          } catch (e) { }

          return {
            id: item.id,
            title: item.activityTitle,
            time: `${item.startDate || ''} ${item.endDate ? '- ' + item.endDate : ''}`,
            location: '馆内展示',
            imageUrl: thumbnails[0] || '/images/activity.png',
            thumbnails: thumbnails
          };
        });

        this.setData({
          activityList: isRefresh ? formattedRecords : this.data.activityList.concat(formattedRecords),
          swiperList: isRefresh ? formattedRecords.slice(0, 3) : this.data.swiperList,
          page: page + 1,
          hasMore: records.length === 10
        });
      }
    } catch (error) {
      console.error('获取活动失败:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  onSwiperChange(e) {
    this.setData({ currentSwiperIndex: e.detail.current });
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/activityDetail/activityDetail?id=${id}`,
    });
  },

  onPullDownRefresh() {
    this.fetchActivityData(true).then(() => {
      wx.stopPullDownRefresh();
    });
  },

  onReachBottom() {
    this.fetchActivityData();
  }
});