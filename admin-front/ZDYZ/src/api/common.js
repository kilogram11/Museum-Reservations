import request from '@/utils/request'

// 图片上传API
export const uploadImageApi = {
  upload(formData) {
    return request({
      url: '/admin/upload/image',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}
