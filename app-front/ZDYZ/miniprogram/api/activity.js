// api/activity.js
import { get, post } from '../utils/request';

// 获取活动列表
export const getActivityList = (data) => {
    return post('/app/home/activity/list', data);
};

// 获取活动详情
export const getActivityDetail = (id) => {
    return get(`/app/home/activity/detail?id=${id}`);
};
