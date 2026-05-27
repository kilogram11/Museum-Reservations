// utils/toast.js
/**
 * 统一的提示框工具函数
 * 兼容低版本基础库，增加参数校验
 */
function showToast(title, icon = 'none', duration = 2000) {
  // 参数校验：title不能为空，且长度限制（小程序showToast的title最多7个汉字）
  if (!title || title.length > 14) {
    console.warn('提示文字不能为空且长度不能超过14个字符');
    title = title?.slice(0, 14) || '操作提示';
  }
  
  // 兼容低版本基础库：如果没有none类型，用success兜底
  const supportNoneIcon = wx.getSystemInfoSync().SDKVersion >= '1.9.0';
  const finalIcon = icon === 'none' && !supportNoneIcon ? 'success' : icon;

  wx.showToast({
    title: title,
    icon: finalIcon,
    duration: duration,
    mask: true // 增加遮罩，提升体验
  });
}

// 简化导出：同时兼容module.exports和exports
module.exports = {
  success: function(title) {
    showToast(title, 'success');
  },
  error: function(title) {
    showToast(title, 'none');
  },
  // 扩展：通用提示（可选）
  toast: showToast
};