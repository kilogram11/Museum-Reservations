<template>
  <!-- 路由出口：所有页面（登录页、管理页等）都会通过这里渲染 -->
  <router-view />
</template>

<script setup>
// 无需额外逻辑，仅作为路由容器
</script>

<style>
/* 全局基础样式，清除默认边距并统一盒模型 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 页面背景（可选，可根据需求调整） */
body {
  background-color: #f5f7fa;
  font-family: "Microsoft YaHei", sans-serif;
}
</style>