// pages/orderRecord/orderRecord.js
import { getMyRecords, cancelBooking, getRecordDetail } from '../../api/booking';

Page({
  data: {
    allOrderList: [], // 存储所有订单
    orderList: [],    // 存储当前显示（筛选后）的订单
    showQRModal: false,
    currentOrder: {},
    loading: false,
    currentTab: 0 // 默认选中“全部”
  },

  onLoad(options) {
    if (options.tab) {
      this.setData({ currentTab: parseInt(options.tab) });
    }
  },

  onShow() {
    this.fetchOrders();
  },

  // 切换标签
  onTabChange(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({ currentTab: index });
    this.filterOrders();
  },

  // 根据当前标签筛选订单
  filterOrders() {
    const { allOrderList, currentTab } = this.data;
    let filtered = [];

    if (currentTab === 0) {
      // 全部
      filtered = allOrderList;
    } else if (currentTab === 1) {
      // 待使用: status=1 (预约成功) && isCheckIn=0 (未核销)
      filtered = allOrderList.filter(item => item.status === 1 && item.isCheckIn === 0);
    } else if (currentTab === 2) {
      // 已核销: isCheckIn=1
      filtered = allOrderList.filter(item => item.isCheckIn === 1);
    } else if (currentTab === 3) {
      // 退款/售后: status=2 (已取消) || isCheckIn=3 (已逾期)
      filtered = allOrderList.filter(item => item.status === 2 || item.isCheckIn === 3);
    }

    this.setData({ orderList: filtered });
  },

  async fetchOrders() {
    try {
      this.setData({ loading: true });
      const res = await getMyRecords();
      if (res.code === 200) {
        const list = res.data || [];
        const formattedList = list.map(item => {
          let name = '参观人';
          try {
            if (item.joinForms) {
              const forms = JSON.parse(item.joinForms);
              name = forms.name || '参观人';
            }
          } catch (e) { }

          return {
            ...item,
            identityName: name,
            isCheckIn: item.joinIsCheckin,
            status: item.joinStatus,
            meetDay: item.joinMeetDay,
            meetTimeStart: item.joinMeetTimeStart,
            meetTimeEnd: item.joinMeetTimeEnd
          };
        }).sort((a, b) => (b.joinAddTime || 0) - (a.joinAddTime || 0));

        this.setData({
          allOrderList: formattedList
        });
        
        // 获取数据后立即筛选
        this.filterOrders();
      }
    } catch (e) {
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 获取详情（包括二维码）
  async showQR(e) {
    const { id } = e.currentTarget.dataset;
    try {
      wx.showLoading({ title: '加载中...' });
      const res = await getRecordDetail(id);
      if (res.code === 200) {
        const item = res.data;
        let name = '参观人';
        try {
          if (item.joinForms) {
            const forms = JSON.parse(item.joinForms);
            name = forms.name || '参观人';
          }
        } catch (e) { }

        this.setData({
          currentOrder: {
            ...item,
            identityName: name,
            isCheckIn: item.joinIsCheckin,
            status: item.joinStatus,
            meetDay: item.joinMeetDay,
            meetTimeStart: item.joinMeetTimeStart,
            meetTimeEnd: item.joinMeetTimeEnd
          },
          showQRModal: true
        });
      }
    } catch (e) {
      wx.showToast({ title: '获取详情失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },

  closeModals() {
    this.setData({ showQRModal: false });
  },

  onCancel(e) {
    const { id, name } = e.currentTarget.dataset;
    wx.showModal({
      title: '确认取消',
      content: `确定要取消"${name}"的预约吗？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            const result = await cancelBooking(id);
            if (result.code === 200) {
              wx.showToast({ title: '已取消', icon: 'success' });
              this.fetchOrders();
            } else {
              wx.showToast({ title: result.msg || '取消失败', icon: 'none' });
            }
          } catch (e) {
            wx.showToast({ title: '操作失败', icon: 'none' });
          }
        }
      }
    });
  },
  // 跳转到场馆位置
  goToLocation() {
    const order = this.data.currentOrder;
    // 使用订单中的经纬度，回退到默认值
    const latitude = order.latitude || 31.86119; // 默认合肥
    const longitude = order.longitude || 117.283042;
    const name = order.museumTitle || '博物馆';
    const address = order.museumAddress || '详细地址';

    // 尝试跳转到小程序内部地图页，如果需要的话
    wx.navigateTo({
      url: `/pages/map/map?lat=${latitude}&lng=${longitude}&title=${name}`,
      fail: () => {
        // 如果跳转失败（比如页面不存在），可以使用原生接口
        wx.openLocation({
          latitude: Number(latitude),
          longitude: Number(longitude),
          name: name,
          address: address,
          scale: 18
        });
      }
    });
  }
});
