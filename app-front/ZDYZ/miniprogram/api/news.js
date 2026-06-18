// api/news.js
import { get, post } from '../utils/request';

// 获取公告列表
export const getNewsList = (data) => {
    return post('/app/home/notice/list', data);
};

// 获取公告详情
export const getNewsDetail = (id) => {
    return get(`/app/home/notice/detail?id=${id}`);
};
