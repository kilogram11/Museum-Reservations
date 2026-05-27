// api/booking.js
import { get, post } from '../utils/request';

// 获取可预约日期
export const getBookingDays = () => {
    return get('/app/booking/days');
};

// 获取指定日期的时段
export const getBookingTimes = (day) => {
    return get(`/app/booking/times?day=${day}`);
};

// 提交预约
export const submitBooking = (data) => {
    return post('/app/booking/submit', data);
};

// 获取我的预约记录
export const getMyRecords = () => {
    return get('/app/record/list');
};

// 取消预约
export const cancelBooking = (joinId) => {
    return post('/app/record/cancel', { joinId });
};

// 获取预约详情及二维码
export const getRecordDetail = (joinId) => {
    return get(`/app/record/detail?joinId=${joinId}`);
};
