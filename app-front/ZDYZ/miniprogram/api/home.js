// api/home.js
import { get } from '../utils/request';

// 获取首页聚合信息（轮播图、今日开放、博物馆简介）
export const getHomeData = () => {
    return get('/app/home/index');
};
