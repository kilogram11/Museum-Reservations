// api/identity.js
import { get, post } from '../utils/request';

// 获取常用游客列表
export const getIdentityList = () => {
    return get('/app/identity/list');
};

// 添加或编辑游客
export const saveIdentity = (data) => {
    return post('/app/identity/save', data);
};

// 删除游客
export const deleteIdentity = (identityId) => {
    return post('/app/identity/del', { identityId });
};
