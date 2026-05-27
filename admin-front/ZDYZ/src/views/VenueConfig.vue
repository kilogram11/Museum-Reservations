<template>
  <div class="venue-config-container">
    <el-card style="padding: 30px; text-align: center;">
    <el-row :gutter="24">
      <el-col :span="12" v-for="item in configItems" :key="item.title" style="margin-bottom: 24px;">
        <el-card class="sio-config-card" shadow="hover" @click="item.action">
           <div class="config-icon-box" :style="{ backgroundColor: item.color + '15' }">
              <el-icon :color="item.color" size="32"><component :is="item.icon" /></el-icon>
           </div>
           <div class="config-info">
              <h3>{{ item.title }}</h3>
              <p>{{ item.desc }}</p>
           </div>
           <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </el-card>
      </el-col>
    </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { OfficeBuilding, Calendar, ChatLineRound, Message, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()

const configItems = [
  { title: '场馆管理', desc: '配置基本信息，预约时间与人数', icon: OfficeBuilding, color: '#ACF44A', action: () => router.push('/admin/museum') },
  { title: '活动管理', desc: '创建、编辑、删除活动内容', icon: Calendar, color: '#fac858', action: () => router.push('/admin/activity') },
  { title: '场馆公告', desc: '发布、维护公告信息', icon: ChatLineRound, color: '#ee6666', action: () => router.push('/admin/news') },
  { title: '消息模版', desc: '配置系统通知文案与变量', icon: Message, color: '#5470c6', action: () => router.push('/admin/message-template') }
]
</script>

<style scoped>
.venue-config-container {
  padding: 40px;
}
.sio-config-card {
  display: flex !important;
  align-items: center;
  gap: 20px;
  padding: 24px !important;
  cursor: pointer;
  transition: all 0.3s;
  text-align: left;
  border-radius: 24px !important;
  border: 1px solid #F4F4F4 !important;
}
:deep(.el-card__body) {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 20px;
}
.config-icon-box {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.config-info {
  flex: 1;
}
.config-info h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #1A1D1F;
}
.config-info p {
  margin: 0;
  color: #6F767E;
  font-size: 13px;
}
.arrow-icon {
  color: #C1C7CD;
  font-size: 20px;
}
.sio-config-card:hover {
  transform: translateY(-4px);
  border-color: #ACF44A !important;
  box-shadow: 0 10px 30px rgba(0,0,0,0.05) !important;
}
</style>