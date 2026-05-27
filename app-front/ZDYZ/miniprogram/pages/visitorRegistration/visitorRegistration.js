// pages/visitorRegistration/visitorRegistration.js
import { getIdentityList, saveIdentity, deleteIdentity } from '../../api/identity';

Page({
  data: {
    newName: '',
    newPhone: '',
    newIdCard: '',
    visitorList: [],
    loading: false,
    editingId: null,
    isEditing: false,
    showForm: false
  },

  onShow() {
    this.fetchVisitorList();
  },

  async fetchVisitorList() {
    try {
      this.setData({ loading: true });
      const res = await getIdentityList();
      if (res.code === 200) {
        const list = (res.data || []).map(item => ({
          id: item.identityId,
          name: item.identityName,
          phone: item.identityMobile,
          idCard: item.identityCard,
          idCardMasked: this.maskIdCard(item.identityCard),
          isBlacklisted: item.identityStatus === 0
        }));
        this.setData({ visitorList: list });
      }
    } catch (error) {
      console.error('获取预约人列表失败:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  onNameInput(e) { this.setData({ newName: e.detail.value }); },
  onPhoneInput(e) { this.setData({ newPhone: e.detail.value }); },
  onIdCardInput(e) { this.setData({ newIdCard: e.detail.value }); },

  // 显隐表单
  showAddForm() {
    this.resetForm();
    this.setData({ showForm: true });
  },

  closeForm() {
    this.resetForm();
    this.setData({ showForm: false });
  },

  async addVisitor() {
    const { newName, newPhone, newIdCard, isEditing, editingId } = this.data;

    if (!newName || !newPhone || !newIdCard) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' });
      return;
    }

    if (!/^1[3-9]\d{9}$/.test(newPhone)) {
      wx.showToast({ title: '手机号格式不正确', icon: 'none' });
      return;
    }

    try {
      wx.showLoading({ title: '保存中...', mask: true });
      const params = {
        identityName: newName,
        identityMobile: newPhone,
        identityCard: newIdCard
      };
      // 如果是编辑，带上唯一标识 ID
      if (isEditing) {
        params.identityId = editingId;
      }

      const res = await saveIdentity(params);
      if (res.code === 200) {
        wx.showToast({ title: '操作成功', icon: 'success' });
        this.setData({ showForm: false });
        this.resetForm();
        this.fetchVisitorList();
      } else {
        wx.showToast({ title: res.msg || '操作失败', icon: 'none' });
      }
    } catch (error) {
      console.error('保存失败:', error);
      wx.showToast({ title: '网络异常', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },

  async deleteVisitor(e) {
    const { id, name } = e.currentTarget.dataset;
    if (!id) return;

    wx.showModal({
      title: '确认删除',
      content: `确定要删除预约人"${name}"吗？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            wx.showLoading({ title: '正在删除...', mask: true });
            const result = await deleteIdentity(id);
            if (result.code === 200) {
              wx.showToast({ title: '已删除', icon: 'success' });
              this.fetchVisitorList();
            } else {
              wx.showToast({ title: result.msg || '删除失败', icon: 'none' });
            }
          } catch (error) {
            console.error('删除操作异常:', error);
            wx.showToast({ title: '删除失败', icon: 'none' });
          } finally {
            wx.hideLoading();
          }
        }
      }
    });
  },

  onEdit(e) {
    const visitor = e.currentTarget.dataset.item;
    this.setData({
      newName: visitor.name,
      newPhone: visitor.phone,
      newIdCard: visitor.idCard,
      editingId: visitor.id,
      isEditing: true,
      showForm: true
    });
  },

  resetForm() {
    this.setData({
      newName: '',
      newPhone: '',
      newIdCard: '',
      editingId: null,
      isEditing: false
      // showForm 由具体调用处控制
    });
  },

  maskIdCard(idCard) {
    if (!idCard || idCard.length < 15) return idCard;
    return idCard.replace(/^(.{6})(?:\d+)(.{4})$/, "$1********$2");
  }
});