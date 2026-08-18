import { post } from '../utils/request';

export const chatWithAi = (message) => {
  return post('/ai/chat', { message });
};
