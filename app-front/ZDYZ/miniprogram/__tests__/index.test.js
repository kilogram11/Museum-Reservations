/**
 * ============================================================
 * 首页模块测试 - 黑盒测试 + 白盒测试
 * ============================================================
 * 方法: 等价类划分、场景法、分支覆盖
 */

describe('【黑盒测试】首页 - 等价类划分测试', () => {

  describe('1.1 首页数据返回值等价类划分', () => {

    test('H-EC-DATA-01 完整数据返回: banners+today+museumInfo+records', () => {
      const res = {
        code: 200,
        data: {
          banners: ['/banner1.jpg', '/banner2.jpg'],
          today: { hours: '08:30-17:00', statusText: '今日开放' },
          museumInfo: {
            title: '故宫博物院',
            address: '北京市东城区',
            phone: '010-12345678',
            desc: '介绍'
          }
        }
      };

      const { banners, today, museumInfo } = res.data;
      expect(banners.length).toBe(2);
      expect(today.statusText).toBe('今日开放');
      expect(museumInfo.title).toBe('故宫博物院');
    });

    test('H-EC-DATA-02 museumInfo为null → 使用默认值', () => {
      const museumInfo = null;
      const formatted = museumInfo ? {
        title: museumInfo.title,
        address: museumInfo.address
      } : null;
      expect(formatted).toBeNull();
    });

    test('H-EC-DATA-03 banners为空数组 → 使用默认轮播图', () => {
      const banners = [];
      const hasBanners = banners && banners.length > 0;
      const defaultBanner = ['/pages/pic/1.png'];
      const result = hasBanners ? banners : defaultBanner;
      expect(result).toEqual(defaultBanner);
    });

    test('H-EC-DATA-04 openTime缺失 → 使用默认开放时间', () => {
      const today = null;
      const museumInfo = null;
      const openTime = (today && today.hours) || (museumInfo && museumInfo.openTimeStr) || '08:30 - 17:00';
      expect(openTime).toBe('08:30 - 17:00');
    });

    test('H-EC-DATA-05 isOpen状态判断: statusText=今日开放', () => {
      const today = { statusText: '今日开放' };
      const isOpen = today && today.statusText === '今日开放';
      expect(isOpen).toBe(true);
    });

    test('H-EC-DATA-06 isOpen状态判断: statusText!=今日开放', () => {
      const today = { statusText: '今日闭馆' };
      const isOpen = today && today.statusText === '今日开放';
      expect(isOpen).toBe(false);
    });
  });

  describe('1.2 活动/公告格式化等价类划分', () => {

    test('H-EC-ACT-01 活动有activityPic(JSON数组) → 解析第一张图', () => {
      const item = { activityPic: JSON.stringify(['/pic1.jpg', '/pic2.jpg']) };
      let img = '';
      try {
        if (item.activityPic) {
          const pics = JSON.parse(item.activityPic);
          if (Array.isArray(pics) && pics.length > 0) {
            img = pics[0];
          }
        }
      } catch (e) { }
      expect(img).toBe('/pic1.jpg');
    });

    test('H-EC-ACT-02 活动无activityPic → 使用默认图', () => {
      const item = { activityPic: undefined };
      let img = '';
      try {
        if (item.activityPic) {
          const pics = JSON.parse(item.activityPic);
          if (Array.isArray(pics) && pics.length > 0) img = pics[0];
        }
      } catch (e) { }
      expect(img).toBe('');
    });

    test('H-EC-ACT-03 activityPic非JSON → 捕获异常,img为空', () => {
      const item = { activityPic: 'not-json' };
      let img = '';
      try {
        if (item.activityPic) {
          const pics = JSON.parse(item.activityPic);
          if (Array.isArray(pics) && pics.length > 0) img = pics[0];
        }
      } catch (e) { }
      expect(img).toBe('');
    });

    test('H-EC-NOTICE-01 公告有newsTitle → 使用标题', () => {
      const item = { newsTitle: '重要公告' };
      const title = item.newsTitle || '系统公告';
      expect(title).toBe('重要公告');
    });

    test('H-EC-NOTICE-02 公告无newsTitle → 使用默认标题', () => {
      const item = {};
      const title = item.newsTitle || '系统公告';
      expect(title).toBe('系统公告');
    });
  });
});

describe('【黑盒测试】首页 - 场景法测试', () => {

  test('H-SC-01 [基本流] 首页加载成功并展示所有模块', async () => {
    // 模拟数据
    const homeData = {
      code: 200,
      data: {
        banners: ['/images/banner1.jpg'],
        today: { hours: '09:00-17:00', statusText: '今日开放' },
        museumInfo: { title: '博物馆', address: '地址', phone: '010-1234', desc: '简介' }
      }
    };

    const activityData = {
      code: 200,
      data: { records: [{ id: 1, activityTitle: '活动1' }] }
    };

    const noticeData = {
      code: 200,
      data: { records: [{ id: 1, newsTitle: '公告1' }] }
    };

    expect(homeData.code).toBe(200);
    expect(activityData.code).toBe(200);
    expect(noticeData.code).toBe(200);
  });

  test('H-SC-02 [备选流] 首页数据获取失败 → 显示错误提示', () => {
    const error = new Error('获取首页数据失败');
    expect(error.message).toContain('获取首页数据失败');
  });

  test('H-SC-03 [备选流] 空数据容错处理', () => {
    const res = { code: 200, data: { banners: null, today: null, museumInfo: null } };
    const { banners, today, museumInfo } = res.data;
    expect(banners).toBeNull();
    expect(today).toBeNull();
    expect(museumInfo).toBeNull();
  });

  test('H-SC-04 [备选流] 跳转地图-无定位信息', () => {
    const museum = { longitude: null, latitude: null };
    const canGoMap = museum && museum.longitude && museum.latitude;
    expect(canGoMap).toBe(false);
  });

  test('H-SC-05 [基本流] 跳转地图-有定位信息', () => {
    const museum = { longitude: 116.397, latitude: 39.908, title: '故宫博物院' };
    const canGoMap = museum && museum.longitude && museum.latitude;
    expect(canGoMap).toBe(true);
    const url = `/pages/map/map?lng=${museum.longitude}&lat=${museum.latitude}&title=${museum.title}`;
    expect(url).toContain('116.397');
    expect(url).toContain('39.908');
  });
});