// pages/identify/identify.js
import { baseUrl } from '../../utils/request';

const app = getApp();

Page({
    data: {
        imagePath: '',
        loading: false,
        result: null
    },

    chooseImage() {
        wx.chooseMedia({
            count: 1,
            mediaType: ['image'],
            sourceType: ['album', 'camera'],
            camera: 'back',
            success: (res) => {
                const tempFilePath = res.tempFiles[0].tempFilePath;
                this.setData({
                    imagePath: tempFilePath,
                    result: null
                });

                // 自动开始识别
                this.uploadAndIdentify(tempFilePath);
            }
        });
    },

    uploadAndIdentify(filePath) {
        this.setData({ loading: true });

        wx.uploadFile({
            url: baseUrl + '/app/relic/identify',
            filePath: filePath,
            name: 'file',
            header: {
                'token': wx.getStorageSync('token') || ''
            },
            success: (res) => {
                try {
                    // wx.uploadFile 返回的是字符串，需要 parse
                    const data = JSON.parse(res.data);

                    if (data.code === 200) {
                        const relicInfo = data.data;
                        // relicInfo 结构: { recognition: {label, id}, detail: {relicName, relicDesc, ...} }

                        const detail = relicInfo.detail;
                        if (detail) {
                            this.setData({
                                result: {
                                    name: detail.relicName || relicInfo.recognition.label, // 优先用数据库名称
                                    desc: detail.relicDesc || '暂无详细介绍'
                                }
                            });
                        } else {
                            // 识别到了ID，但数据库没资料
                            this.setData({
                                result: {
                                    name: relicInfo.recognition.label,
                                    desc: '识别成功，但数据库中暂无该文物的详细介绍。'
                                }
                            });
                        }

                        // 如果后端返回了 3D 模型 URL，跳转到 3D 展示页
                        if (relicInfo.modelUrl) {
                            wx.showModal({
                                title: '发现 3D 模型',
                                content: `识别到"${relicInfo.recognition.label}"的 3D 模型，是否查看？`,
                                confirmText: '立即查看',
                                cancelText: '稍后',
                                success: (res) => {
                                    if (res.confirm) {
                                        wx.navigateTo({
                                            url: `/pages/relic3d/relic3d?modelUrl=${encodeURIComponent(relicInfo.modelUrl)}&name=${encodeURIComponent(detail.relicName || relicInfo.recognition.label)}&desc=${encodeURIComponent(detail.relicDesc || '暂无介绍')}`
                                        });
                                    }
                                }
                            });
                        }
                    } else {
                        wx.showToast({
                            title: data.msg || '未能识别到文物信息',
                            icon: 'none',
                            duration: 3000
                        });
                        this.setData({ result: null });
                    }
                } catch (e) {
                    console.error(e);
                    wx.showToast({ title: '解析结果失败', icon: 'none' });
                }
            },
            fail: (err) => {
                console.error(err);
                wx.showToast({ title: '网络请求失败', icon: 'none' });
            },
            complete: () => {
                this.setData({ loading: false });
            }
        });
    },

    reset() {
        this.setData({
            imagePath: '',
            result: null
        });
        this.chooseImage();
    }
});
