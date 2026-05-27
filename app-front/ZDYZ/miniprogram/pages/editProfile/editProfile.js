import { updateUserInfo, getHeads } from '../../api/user';
const app = getApp();

Page({
  data: {
    avatarList: [],
    selectedAvatarUrl: '',
    selectedAvatarId: 1,
    userName: '',
    loading: false
  },

  onLoad() {
    this.fetchHeadList();
    this.initUserInfo();
  },

  async fetchHeadList() {
    try {
      const res = await getHeads();
      if (res.code === 200) {
        const list = (res.data || []).map(item => ({
          id: item.id,
          url: item.headPicUrl
        }));
        this.setData({ avatarList: list });
      }
    } catch (e) { }
  },

  initUserInfo() {
    const userInfo = wx.getStorageSync('userInfo');
    if (userInfo) {
      this.setData({
        userName: userInfo.name || '',
        selectedAvatarId: userInfo.userPic || 1,
        selectedAvatarUrl: userInfo.avatarUrl || '/images/avatars/1.png'
      });
    }
  },

  selectAvatar(e) {
    const { id, url } = e.currentTarget.dataset;
    this.setData({
      selectedAvatarId: id,
      selectedAvatarUrl: url
    });
  },

  onUserNameInput(e) {
    this.setData({ userName: e.detail.value });
  },

  async saveProfile() {
    const { userName, selectedAvatarId, loading } = this.data;
    if (loading) return;
    if (!userName.trim()) return wx.showToast({ title: '请输入昵称', icon: 'none' });

    try {
      this.setData({ loading: true });
      wx.showLoading({ title: '保存中...' });

      const res = await updateUserInfo({
        userName: userName.trim(),
        userPic: selectedAvatarId
      });

      wx.hideLoading(); // 先手动关闭加载

      if (res.code === 200) {
        // 更新本地存储
        const userInfo = wx.getStorageSync('userInfo') || {};
        userInfo.name = userName.trim();
        userInfo.userPic = selectedAvatarId;
        userInfo.avatarUrl = this.data.selectedAvatarUrl;
        wx.setStorageSync('userInfo', userInfo);

        wx.showToast({
          title: '修改成功',
          icon: 'success',
          complete: () => {
            setTimeout(() => { wx.navigateBack(); }, 1500);
          }
        });
      } else {
        wx.showToast({ title: res.msg || '修改失败', icon: 'none' });
      }
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: '网络异常', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  goBack() { wx.navigateBack(); }
});