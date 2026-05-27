// pages/activityDetail/activityDetail.js
import { getActivityDetail } from '../../api/activity';
import { baseUrl } from '../../utils/request';

Page({
  data: {
    detail: {},
    loading: false
  },

  // 在 Page 中增加预览方法
previewImage(e) {
  const current = e.currentTarget.dataset.src;
  const urls = this.data.detail.contentList
    .filter(i => i.type === 'img' || i.type === 'image')
    .map(i => i.val);
    
  wx.previewImage({
    current,
    urls
  });
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
      const res = await getActivityDetail(id);
      if (res.code === 200) {
        const item = res.data;
        // 解析内容
        let contentList = [];
        try {
          if (item.activityObj) {
            const obj = JSON.parse(item.activityObj);
            // 处理内容列表中的图片路径
            contentList = (obj.content || []).map(c => {
               if ((c.type === 'img' || c.type === 'image') && c.val && c.val.startsWith('/')) {
                   return { ...c, val: baseUrl + c.val }
               }
               return c
            });
          }
        } catch (e) { }

        this.setData({
          detail: {
            title: item.activityTitle,
            activityTime: `${item.startDate || ''} ${item.endDate ? '- ' + item.endDate : ''}`,
            address: '馆内展区',
            publishTime: item.activityAddTime ? new Date(item.activityAddTime).toLocaleDateString() : '',
            museumName: '博物馆',
            province: '官方发布',
            registerMethod: '在线预约或现场参与',
            footerNote: '温馨提示：请按时参加。',
            contentList: contentList,
            description: contentList.filter(c => c.type === 'text').map(c => c.val).join('\n'),
            description: contentList.filter(c => c.type === 'text').map(c => c.val).join('\n'),
            // coverImage 已在 contentList 处理中被替换，或者如果单独取，也要处理
            coverImage: contentList.find(c => (c.type === 'img' || c.type === 'image'))?.val || ''
          }
        });
      }
    } catch (error) {
      console.error('获取详情失败:', error);
      wx.showToast({ title: '加载详情失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  }
});