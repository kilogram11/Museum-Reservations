/**
 * ============================================================
 * API 模块黑盒测试 - 等价类划分 + 边界值分析
 * ============================================================
 * 测试策略:
 *   1. 等价类划分: 将输入域划分为有效等价类和无效等价类
 *   2. 边界值分析: 对边界值进行专门测试
 *   3. 场景法: 覆盖正常流程和异常流程
 */

const mockRequest = jest.fn();

// Mock wx.request
wx.request.mockImplementation(mockRequest);

// 重新导入前重置
jest.resetModules();

// 动态导入 API 模块
const activityApi = require('../../api/activity');
const bookingApi = require('../../api/booking');
const homeApi = require('../../api/home');
const identityApi = require('../../api/identity');
const newsApi = require('../../api/news');
const userApi = require('../../api/user');

describe('【黑盒测试】API 模块 - 等价类划分测试', () => {

  beforeEach(() => {
    mockRequest.mockClear();
    wx.getStorageSync.mockClear();
    global.__storage = {};
  });

  // ============================================================
  // 1. 等价类划分测试
  // ============================================================

  describe('1.1 API 请求参数等价类划分', () => {

    test('A-EC-01 [活动列表] page=1(有效), limit=10(有效) → 期望正常请求', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 200, data: { code: 200, data: { records: [] } } });
      });
      wx.getStorageSync.mockReturnValue('mock-token');

      const result = await activityApi.getActivityList({ page: 1, limit: 10 });
      expect(result.code).toBe(200);
    });

    test('A-EC-02 [活动列表] page=0(无效-边界), limit=10 → 期望请求发出', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 200, data: { code: 200, data: { records: [] } } });
      });
      const result = await activityApi.getActivityList({ page: 0, limit: 10 });
      expect(result).toBeDefined();
    });

    test('A-EC-03 [公告列表] 无参数 → 期望使用默认值', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 200, data: { code: 200, data: { records: [] } } });
      });
      const result = await newsApi.getNewsList({});
      expect(result.code).toBe(200);
    });

    test('A-EC-04 [获取活动详情] id=有效字符串 → 期望正常请求', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 200, data: { code: 200, data: { id: 1, activityTitle: 'test' } } });
      });
      const result = await activityApi.getActivityDetail('1');
      expect(result.code).toBe(200);
    });

    test('A-EC-05 [获取活动详情] id=""(空字符串) → 期望请求发出', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 200, data: { code: 200, data: null } });
      });
      const result = await activityApi.getActivityDetail('');
      expect(result).toBeDefined();
    });

    test('A-EC-06 [登录] mobile=13812345678(有效), code=1234(有效) → 期望成功', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 200, data: { code: 200, data: { token: 'test-token' } } });
      });
      const result = await userApi.login('13812345678', '1234');
      expect(result.code).toBe(200);
    });

    test('A-EC-07 [登录] mobile=空, code=空 → 期望请求发出(校验在页面层)', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 200, data: { code: 400, msg: '参数错误' } });
      });
      const result = await userApi.login('', '');
      expect(result).toBeDefined();
    });
  });

  // ============================================================
  // 2. 响应处理等价类划分
  // ============================================================

  describe('1.2 API 响应处理等价类划分', () => {

    test('A-REC-01 HTTP 200 + code=200 → 正常返回数据', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 200, data: { code: 200, data: { records: [] } } });
      });
      const result = await activityApi.getActivityList({ page: 1, limit: 10 });
      expect(result.code).toBe(200);
    });

    test('A-REC-02 HTTP 200 + code!=200 → 返回错误信息', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 200, data: { code: 400, msg: '业务错误' } });
      });
      const result = await activityApi.getActivityList({ page: 1, limit: 10 });
      expect(result.code).toBe(400);
    });

    test('A-REC-03 HTTP 401 → 抛出认证异常', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 401, data: {} });
      });
      await expect(activityApi.getActivityList({})).rejects.toThrow('登录已过期，请重新登录');
    });

    test('A-REC-04 HTTP 403 → 抛出权限异常', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 403, data: {} });
      });
      await expect(activityApi.getActivityList({})).rejects.toThrow('无权限访问');
    });

    test('A-REC-05 HTTP 500 → 抛出服务端错误', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 500, data: {} });
      });
      await expect(activityApi.getActivityList({})).rejects.toThrow('HTTP错误: 500');
    });

    test('A-REC-06 网络请求失败(fail回调) → 抛出网络异常', async () => {
      mockRequest.mockImplementation(({ fail }) => {
        fail(new Error('网络连接失败'));
      });
      await expect(activityApi.getActivityList({})).rejects.toThrow('网络请求失败');
    });

    test('A-REC-07 响应数据为null → 返回null', async () => {
      mockRequest.mockImplementation(({ success }) => {
        success({ statusCode: 200, data: null });
      });
      await expect(activityApi.getActivityList({})).rejects.toThrow('响应数据为空');
    });
  });
});

describe('【黑盒测试】API 模块 - 边界值分析测试', () => {

  beforeEach(() => {
    mockRequest.mockClear();
  });

  test('A-BVA-01 [手机号] 最小有效长度: 11位', async () => {
    mockRequest.mockImplementation(({ success }) => {
      success({ statusCode: 200, data: { code: 200, data: { token: 'test' } } });
    });
    const result = await userApi.login('13812345678', '1234');
    expect(result.code).toBe(200);
  });

  test('A-BVA-02 [手机号] 10位(无效-边界)', () => {
    // 页面层校验：/^1[3-9]\d{9}$/ 需要11位
    const regex = /^1[3-9]\d{9}$/;
    expect(regex.test('1381234567')).toBe(false);  // 10位
    expect(regex.test('13812345678')).toBe(true);   // 11位
    expect(regex.test('138123456789')).toBe(false); // 12位
  });

  test('A-BVA-03 [手机号] 首位校验: 1[3-9]有效', () => {
    const regex = /^1[3-9]\d{9}$/;
    expect(regex.test('13012345678')).toBe(true);  // 13开头
    expect(regex.test('19012345678')).toBe(true);  // 19开头
    expect(regex.test('12012345678')).toBe(false); // 12开头-无效
    expect(regex.test('11012345678')).toBe(false); // 11开头-无效
  });

  test('A-BVA-04 [分页] page=1, limit=1(最小)', async () => {
    mockRequest.mockImplementation(({ success }) => {
      success({ statusCode: 200, data: { code: 200, data: { records: [{ id: 1 }] } } });
    });
    const result = await activityApi.getActivityList({ page: 1, limit: 1 });
    expect(result.data.records).toHaveLength(1);
  });

  test('A-BVA-05 [分页] limit=0(无效边界)', async () => {
    mockRequest.mockImplementation(({ success }) => {
      success({ statusCode: 200, data: { code: 200, data: { records: [] } } });
    });
    // 虽然limit=0不合理，但api层不校验，请求正常发出
    const result = await activityApi.getActivityList({ page: 1, limit: 0 });
    expect(result).toBeDefined();
  });
});

describe('【黑盒测试】API 模块 - Token认证场景测试', () => {

  beforeEach(() => {
    mockRequest.mockClear();
    wx.getStorageSync.mockClear();
    global.__storage = {};
  });

  test('A-AUTH-01 有Token → Header中携带Token', async () => {
    wx.getStorageSync.mockReturnValue('valid-token-123');
    mockRequest.mockImplementation(({ header, success }) => {
      expect(header.Token || header.token).toBeDefined();
      success({ statusCode: 200, data: { code: 200, data: {} } });
    });
    await userApi.getUserInfo();
  });

  test('A-AUTH-02 无Token → Header不携带Token', async () => {
    wx.getStorageSync.mockReturnValue('');
    mockRequest.mockImplementation(({ header, success }) => {
      expect(header.Token).toBeUndefined();
      success({ statusCode: 200, data: { code: 200, data: {} } });
    });
    await homeApi.getHomeData();
  });
});