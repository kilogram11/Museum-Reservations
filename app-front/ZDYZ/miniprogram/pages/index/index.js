import { getHomeData } from '../../api/home';
import { getActivityList } from '../../api/activity';
import { getNewsList } from '../../api/news';
import { baseUrl } from '../../utils/request';

Page({
  data: {
    todayDate: '',
    openTime: '',
    isOpen: true,
    bannerList: [],
    museum: null,
    loading: true,
    activityList: [], // 馆内活动
    noticeList: []    // 馆内公告
  },

  onLoad(options) {
    this.fetchHomeData();
    this.setCurrentDate();
  },

  goMap() {
    const { museum } = this.data;

    // ✅ 方法A：打印经纬度
    console.log('museum 对象 =', museum);
    console.log('latitude:', museum?.latitude, 'longitude:', museum?.longitude);

    if (!museum || !museum.longitude || !museum.latitude) {
      wx.showToast({
        title: '暂无场馆定位信息',
        icon: 'none'
      });
      return;
    }

    wx.navigateTo({
      url: `/pages/map/map?lng=${museum.longitude}&lat=${museum.latitude}&title=${museum.title}`
    });
  },


  onPullDownRefresh() {
    this.fetchHomeData().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  setCurrentDate() {
    const now = new Date();
    const month = now.getMonth() + 1;
    const day = now.getDate();
    this.setData({ todayDate: `${month}月${day}日` });
  },

  async fetchHomeData() {
    try {
      this.setData({ loading: true });
      const res = await getHomeData();
      const activityRes = await getActivityList({ page: 1, limit: 10 });
      const noticeRes = await getNewsList({ page: 1, limit: 5 });

      if (res.code === 200) {
        const { banners, today, museumInfo } = res.data;

        // 兼容处理：后端返回的是 phone，前端使用的是 contact
        const formattedMuseum = museumInfo ? {
          ...museumInfo,
          title: museumInfo.title || '博物馆',
          address: museumInfo.address || '暂无详细地址',
          contact: museumInfo.phone || '暂无电话',
          desc: museumInfo.desc || '欢迎点击查看更多详情。',
          openTimeStr: museumInfo.openTimeStr || '08:30 - 17:00',
          latitude: museumInfo.latitude || museumInfo.lat || null,
          longitude: museumInfo.longitude || museumInfo.lng || null
        } : null;

        const activityRecords = activityRes.code === 200 ? (activityRes.data.records || []) : [];
        const noticeRecords = noticeRes.code === 200 ? (noticeRes.data.records || []) : [];

        this.setData({
          bannerList: (banners && banners.length > 0)
            ? banners.map(url => url.startsWith('/') ? baseUrl + url : url)
            : ['/pages/pic/1.png'],
          openTime: (today && today.hours) || (formattedMuseum && formattedMuseum.openTimeStr) || '08:30 - 17:00',
          isOpen: today && today.statusText === '今日开放',
          museum: formattedMuseum,
          noticeList: noticeRecords.map(item => ({
            ...item,
            title: item.newsTitle || '系统公告'
          })),
          activityList: activityRecords.map(item => {
            let img = '';
            try {
              if (item.activityPic) {
                const pics = JSON.parse(item.activityPic);
                if (Array.isArray(pics) && pics.length > 0) {
                  img = pics[0];
                }
              }
            } catch (e) { }
            return {
              ...item,
              title: item.activityTitle || '馆内活动',
              image: img ? (img.startsWith('http') ? img : baseUrl + img) : ''
            };
          })
        });
      }
    } catch (error) {
      console.error('获取首页数据失败:', error);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  goToReserve() {
    wx.navigateTo({
      url: '/pages/noticeReservation/noticeReservation'
    });
  },

  goToActivity() {
    wx.navigateTo({
      url: '/pages/activity/activity'
    });
  },

  goToActivityDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/activityDetail/activityDetail?id=${id}`
    });
  },

  goToNotice() {
    wx.navigateTo({
      url: '/pages/notice/notice'
    });
  },

  goToNoticeDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/noticeDetail/noticeDetail?id=${id}`
    });
  }
})
