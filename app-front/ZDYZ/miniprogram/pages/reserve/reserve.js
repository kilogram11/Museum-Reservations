// pages/reserve/reserve.js
import { getBookingDays, getBookingTimes, submitBooking } from '../../api/booking';
import { getIdentityList } from '../../api/identity';
import { getHomeData } from '../../api/home';

Page({
  data: {
    currentMonth: '',
    selectedDateFull: '',
    selectedDateIndex: -1,
    selectedSlotIndex: -1,
    dateList: [],
    timeSlots: [],
    visitors: [],
    selectedVisitorIds: [],
    loading: false,
    isLoggedIn: false,
    museumInfo: {},
    selectedTimeMark: '',
    submitting: false,
    errorMessage: ''
  },

  onLoad() {
    this.checkLoginStatus();
    this.fetchMuseumInfo();
  },

  onShow() {
    this.checkLoginStatus();
    this.fetchAvailableDates();
    if (this.data.isLoggedIn) {
      this.fetchVisitors();
    }
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token');
    const isLoggedIn = !!token;
    this.setData({ isLoggedIn });
    return isLoggedIn;
  },

  async fetchMuseumInfo() {
    try {
      const res = await getHomeData();
      if (res.code === 200) {
        this.setData({ museumInfo: res.data.museumInfo });
      }
    } catch (e) { console.error(e); }
  },

  async fetchAvailableDates() {
    try {
      this.setData({ loading: true, errorMessage: '' });
      const res = await getBookingDays();
      if (res.code === 200 && Array.isArray(res.data)) {
        const formattedDates = res.data.map(item => {
          const d = new Date(item.day);
          return {
            dateStr: item.day,
            monthStr: `${d.getMonth() + 1}月`,
            dayStr: d.getDate(),
            weekStr: item.week,
            status: item.status === 1 ? 'available' : (item.status === 2 ? 'full' : 'closed'),
            statusText: item.status === 1 ? '可预约' : (item.status === 2 ? '已约满' : '闭馆')
          };
        });
        this.setData({ dateList: formattedDates });

        // 自动选择第一个可约日期
        const firstAvail = formattedDates.findIndex(d => d.status === 'available');
        if (firstAvail !== -1) {
          this.onSelectDate(firstAvail);
        }
      }
    } catch (error) {
      this.setData({ errorMessage: '获取日期失败' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onSelectDate(index) {
    const dateItem = this.data.dateList[index];

    // 【修改点 1】日期不可用时的弹窗提示
    if (dateItem.status !== 'available') {
      const msg = dateItem.status === 'full' ? '该日期本次预约已满，请选择其他日期。' : '该日期闭馆，请选择其他日期。';
      wx.showModal({
        title: '无法预约',
        content: msg,
        showCancel: false,
        confirmText: '知道了'
      });
      return;
    }

    this.setData({
      selectedDateIndex: index,
      selectedDateFull: dateItem.dateStr,
      selectedSlotIndex: -1,
      selectedTimeMark: '',
      timeSlots: []
    });

    try {
      this.setData({ loading: true });
      const res = await getBookingTimes(dateItem.dateStr);
      if (res.code === 200) {
        this.setData({
          timeSlots: res.data.map(item => ({
            ...item,
            time: `${item.startTime}-${item.endTime}`,
            remaining: item.surplus
          }))
        });
      }
    } catch (e) {
      wx.showToast({ title: '获取时段失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 这里的 selectDate 是给 wxml 调用的包装
  selectDate(e) {
    this.onSelectDate(e.currentTarget.dataset.index);
  },

  async fetchVisitors() {
    try {
      const res = await getIdentityList();
      if (res.code === 200) {
        this.setData({
          visitors: (res.data || []).map(v => ({
            id: v.identityId,
            name: v.identityName,
            phone: v.identityMobile,
            idCardMasked: v.identityCard.replace(/^(.{6})(?:\d+)(.{4})$/, "$1********$2"),
            selected: false,
            isBlacklisted: v.identityStatus === 0
          }))
        });
      }
    } catch (e) { console.error(e); }
  },

  selectSlot(e) {
    const index = e.currentTarget.dataset.index;
    const slot = this.data.timeSlots[index];

    // 【修改点 2】名额不足时的弹窗提示
    if (slot.remaining <= 0) {
      wx.showModal({
        title: '此时段已满',
        content: '该时段剩余名额为0，请选择其他时段。',
        showCancel: false,
        confirmText: '知道了'
      });
      return;
    }

    this.setData({
      selectedSlotIndex: index,
      selectedTimeMark: slot.timeMark
    });
  },

  toggleVisitor(e) {
    if (!this.data.isLoggedIn) return wx.navigateTo({ url: '/pages/login/login' });
    const id = e.currentTarget.dataset.id;
    const { visitors, selectedVisitorIds } = this.data;
    const vIndex = visitors.findIndex(v => v.id === id);
    if (vIndex === -1) return;

    const visitor = visitors[vIndex];
    if (visitor.isBlacklisted) {
      return wx.showToast({ title: '该客人在黑名单中', icon: 'none' });
    }

    if (!visitor.selected && selectedVisitorIds.length >= 3) {
      return wx.showToast({ title: '一次最多预约3人', icon: 'none' });
    }

    visitor.selected = !visitor.selected;
    const newSelectedIds = visitors.filter(v => v.selected).map(v => v.id);
    this.setData({ visitors, selectedVisitorIds: newSelectedIds });
  },

  goToAddVisitor() {
    wx.navigateTo({ url: '/pages/visitorRegistration/visitorRegistration' });
  },

  async submitReserve() {
    const { selectedTimeMark, selectedVisitorIds, submitting } = this.data;
    if (!this.data.isLoggedIn) return wx.navigateTo({ url: '/pages/login/login' });
    if (!selectedTimeMark) return wx.showToast({ title: '请选择时段', icon: 'none' });
    if (selectedVisitorIds.length === 0) return wx.showToast({ title: '请选择预约人', icon: 'none' });
    if (submitting) return;

    try {
      this.setData({ submitting: true });
      wx.showLoading({ title: '提交中...' });
      const res = await submitBooking({
        timeMark: selectedTimeMark,
        identityIds: selectedVisitorIds
      });
      if (res.code === 200) {
        wx.showToast({
          title: '预约成功',
          icon: 'success',
          complete: () => {
            // 设置成功预约标识，以便 profile 页面自动打开记录列表，且路径关系变为 Profile -> Record
            wx.setStorageSync('fromReserveSuccess', true);
            setTimeout(() => { wx.switchTab({ url: '/pages/profile/profile' }); }, 1000);
          }
        });
      } else {
        // 后端返回的业务错误（如200 OK但 code!=200）
        wx.showModal({
          title: '预约失败',
          content: res.msg || '未知错误',
          showCancel: false
        });
      }
    } catch (e) {
      // 【修改点 3】捕获的异常（如HTTP 500或BusinessException）
      console.error('Submit Error Catch:', e);
      let errorMsg = '提交失败，请稍后重试';
      if (e && e.message) {
        // 清洗错误信息
        errorMsg = e.message.replace('HTTP错误: ', '').replace('Error: ', '');
      } else if (typeof e === 'string') {
        errorMsg = e;
      }

      wx.showModal({
        title: '预约失败',
        content: errorMsg,
        showCancel: false,
        confirmText: '知道了'
      });
    } finally {
      this.setData({ submitting: false });
      wx.hideLoading();
    }
  },

  onPullDownRefresh() {
    this.fetchAvailableDates();
    if (this.data.isLoggedIn) this.fetchVisitors();
    wx.stopPullDownRefresh();
  }
});