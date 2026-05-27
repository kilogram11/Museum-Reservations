import { get, post } from "../../utils/request";

const app = getApp();

Page({
  data: {
    phone: "", // 手机号 - 对应wxml中的phone
    code: "", // 验证码
    countDown: 0, // 倒计时剩余秒数
    canSendCode: true, // 是否可发送验证码
  },

  // 输入手机号 - 对应wxml中的bindinput="onPhoneInput"
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value });
  },

  // 输入验证码 - 对应wxml中的bindinput="onCodeInput"
  onCodeInput(e) {
    this.setData({ code: e.detail.value });
  },

  // 发送验证码 - 对应wxml中的bindtap="sendCode"
  sendCode() {
    const { phone, canSendCode } = this.data;

    // 1. 校验手机号格式
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({ title: "请输入正确的手机号", icon: "none" });
      return;
    }

    // 2. 若倒计时中，禁止重复点击
    if (!canSendCode) return;

    // 3. 模拟发送验证码（根据API文档，验证码固定为1234）
    wx.showToast({
      title: "验证码已发送：1234",
      icon: "none",
      duration: 3000,
    });

    // 4. 开始倒计时（60秒）
    this.setData({
      countDown: 60,
      canSendCode: false,
    });

    // 5. 每秒更新倒计时
    const timer = setInterval(() => {
      const { countDown } = this.data;
      if (countDown <= 1) {
        // 倒计时结束
        clearInterval(timer);
        this.setData({
          countDown: 0,
          canSendCode: true,
        });
      } else {
        this.setData({ countDown: countDown - 1 });
      }
    }, 1000);
  },

  // 登录/注册 - 对应wxml中的bindtap="doLogin"
  async doLogin() {
    const { phone, code } = this.data;

    if (!phone || !code) {
      wx.showToast({ title: "请填写完整信息", icon: "none" });
      return;
    }

    // 验证码校验（根据API文档，测试验证码固定为1234）
    if (code !== "1234") {
      wx.showToast({ title: "验证码错误，请输入1234", icon: "none" });
      return;
    }

    try {
      // 1. 调用登录/注册接口
      wx.showLoading({ title: "登录中..." });

      // 这里需要替换为实际的API调用
      const loginResult = await post("/app/user/login", {
        mobile: phone,
        code,
      });

      wx.hideLoading();

      // 2. 处理登录结果
      if (loginResult.code !== 200) {
        throw new Error(loginResult.msg || "登录失败");
      }

      // 3. 保存Token
      const token = loginResult.data.token;
      wx.setStorageSync("token", token);

      // 4. 获取用户信息
      wx.showLoading({ title: "获取用户信息..." });

      // 稍微延迟一下确保 token 存储稳定
      await new Promise((r) => setTimeout(r, 100));

      const userInfoResult = await get("/app/user/info");

      wx.hideLoading();

      if (userInfoResult.code !== 200) {
        throw new Error(userInfoResult.msg || "获取用户信息失败");
      }

      // 5. 格式化并保存用户信息
      const userData = userInfoResult.data;
      const phoneLast4 = phone.slice(-4);
      const userInfo = {
        _id: userData._id,
        userId: userData.userId,
        name: userData.userName || `用户${phoneLast4}`, // 对应app.globalData.userInfo中的name
        phone: phone, // 对应app.globalData.userInfo中的phone
        avatarEmoji: "😀", // 保留原有的emoji头像
        userPic: userData.userPic || 0,
      };

      // 6. 更新全局状态
      app.globalData.isLogin = true;
      app.globalData.userInfo = userInfo;
      app.globalData.token = token;

      // 7. 保存到本地存储
      wx.setStorageSync("isLogin", true);
      wx.setStorageSync("userInfo", userInfo);
      wx.setStorageSync("token", token);

      // 8. 登录成功提示
      wx.showToast({
        title: "登录成功",
        icon: "success",
        duration: 1500,
      });

      // 9. 跳转到首页（使用reLaunch以确保彻底重置页面栈，解决switchTab可能失效的问题）
      setTimeout(() => {
        wx.reLaunch({
          url: "/pages/index/index",
          fail: (err) => {
            console.error("跳转失败:", err);
            wx.showToast({
              title: "跳转失败，请手动重启",
              icon: "none",
            });
          },
        });
      }, 1500);
    } catch (error) {
      wx.hideLoading();
      console.error("登录失败:", error);
      wx.showToast({
        title: error.message || "登录失败，请重试",
        icon: "none",
        duration: 2000,
      });

      // 开发模式下显示详细错误
      if (typeof wx.getAccountInfoSync === "function") {
        const accountInfo = wx.getAccountInfoSync();
        if (accountInfo.miniProgram.envVersion === "develop") {
          console.log("详细错误信息:", error);
        }
      }
    }
  },

  // 已改用 utils/request 中的 post 方法进行 API 调用，无需自定义 apiRequest

  // 页面生命周期
  onShow() {
    // 移除自动跳转逻辑，确保用户能看到登录页面
    // 仅在需要时清理旧状态
  },
});
