<template>
  <div class="sio-container">
    <el-row :gutter="24">
      <!-- 左侧区域 (8) -->
      <el-col :span="8">
        <!-- 管理员名片面板 (Update 风格) -->
        <el-card class="update-banner-card">
          <div class="update-header">
             <div class="dot green-dot"></div>
             <span>管理员概览</span>
          </div>
          <div class="admin-profile-mini">
            <el-avatar :size="56" :src="adminProfile.currentAvatar" />
            <div class="admin-meta-info">
               <div class="admin-name">{{ adminProfile.userName }}</div>
               <div class="admin-role">系统管理员</div>
            </div>
          </div>
          <div class="admin-intro-text">
            职责：{{ adminProfile.userIntro || '负责博物馆日常运营与系统维护' }}
          </div>
        </el-card>

        <!-- 6个快捷入口 -->
        <el-card class="sio-card" shadow="never">
          <div class="card-title-mini">快捷办公</div>
          <div class="sio-shortcuts">
             <div v-for="item in shortcuts" :key="item.label" class="sio-shortcut-tile" @click="$router.push(item.path)">
                <div class="icon-wrap" :style="{ backgroundColor: item.bg + '22' }">
                  <el-icon :style="{ color: item.bg }"><component :is="item.icon" /></el-icon>
                </div>
                <span>{{ item.label }}</span>
             </div>
          </div>
        </el-card>

        <!-- 核销状态图表 -->
        <el-card class="sio-card no-border mt-24" shadow="never">
          <div class="card-title-mini">今日核销状态</div>
          <div ref="checkinStatusChartRef" class="mini-chart-pie"></div>
        </el-card>
      </el-col>

      <!-- 右侧区域 (16) -->
      <el-col :span="16">
        <div class="dashboard-intro">
           <h2>首页总览</h2>
           <p>欢迎回来，您可以快速查看博物馆预约数据生命周期并进行管理操作。</p>
        </div>

        <!-- 4个统计卡片 (2x2) -->
        <el-row :gutter="24" class="stat-grid-row">
          <el-col :span="12" v-for="item in statItems" :key="item.title">
            <el-card class="stat-flat-card" shadow="hover">
               <div class="flat-header">
                  <span>{{ item.title }}</span>
                  <el-icon><InfoFilled /></el-icon>
               </div>
               <div class="flat-value-row">
                  <div class="flat-value">{{ formatNum(item.value) }}</div>
               </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 未来7日预约趋势 -->
        <el-card class="sio-card mt-24" shadow="never">
           <div class="chart-header-row">
              <h3>未来7日预约趋势</h3>
              <div class="date-picker-fake">
                 <el-icon><Calendar /></el-icon>
                 <span>最近一周</span>
              </div>
           </div>
           <div ref="trendChartRef" class="main-chart-sio"></div>
        </el-card>

        <!-- 下方图表格子 -->
        <el-row :gutter="24" class="mt-24">
           <el-col :span="12">
              <el-card class="sio-card" shadow="never">
                 <div class="card-title-mini">热门资讯阅读排行</div>
                 <div ref="popularNewsChartRef" class="side-chart"></div>
              </el-card>
           </el-col>
           <el-col :span="12">
              <el-card class="sio-card" shadow="never">
                 <div class="card-title-mini">爽约人数对比 (本周)</div>
                 <div ref="noShowComparisonChartRef" class="side-chart"></div>
              </el-card>
           </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getStatsApi } from '@/api/stats'
import { profileApi } from '@/api/profile'
import { 
  User, CircleCheck, Clock, WarningFilled, 
  Tickets, OfficeBuilding, Calendar, Notification, Document, Remove,
  InfoFilled, MoreFilled
} from '@element-plus/icons-vue'

const adminProfile = reactive({ userName: '管理员', userIntro: '正在加载...', currentAvatar: '' })
const stats = ref({ todayTotalBooking: 0, todayChecked: 0, todayUnchecked: 0, weekNoShow: 0 })

const statItems = computed(() => [
  { title: '总预约人数', value: stats.value.todayTotalBooking, trend: 12, color: '#ACF44A' },
  { title: '今日已核销', value: stats.value.todayChecked, trend: 5, color: '#ACF44A' },
  { title: '未核销人数', value: stats.value.todayUnchecked, trend: -2, color: '#FAC858' },
  { title: '本周爽约人数', value: stats.value.weekNoShow, trend: 8, color: '#FF5E5E' },
])

const shortcuts = [
  { label: '预约核销', path: '/admin/join', icon: Tickets, bg: '#ACF44A' },
  { label: '场馆配置', path: '/admin/museum-config', icon: OfficeBuilding, bg: '#fac858' },
  { label: '活动管理', path: '/admin/activity', icon: Calendar, bg: '#ee6666' },
  { label: '公告管理', path: '/admin/news', icon: Notification, bg: '#5470c6' },
  { label: '通知模板', path: '/admin/template', icon: Document, bg: '#91cc75' },
  { label: '黑名单', path: '/admin/blacklist', icon: Remove, bg: '#73c0de' }
]

// 图表引用
const trendChartRef = ref(null)
const checkinStatusChartRef = ref(null)
const popularNewsChartRef = ref(null)
const noShowComparisonChartRef = ref(null)

let charts = []

const formatNum = (v) => v?.toLocaleString() || '0'

const initAdmin = async () => {
  const res = await profileApi.getProfile()
  if (res.code === 200) Object.assign(adminProfile, res.data)
}

const getSioColor = (i) => ['#0B2118', '#ACF44A', '#F7F8FA', '#FAC858', '#FF5E5E', '#73C0DE'][i % 6]

// 初始化趋势图
const initTrend = async () => {
  const res = await getStatsApi.getTrend()
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  chart.setOption({
    color: ['#0B2118', '#ACF44A'],
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderRadius: 12 },
    legend: { right: 10, top: 0 },
    grid: { top: '15%', left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: res.data.map(i => i.date), axisLine: { show: false }, axisTick: { show: false } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    series: [
      { name: '总名额', type: 'line', data: res.data.map(i => i.total), smooth: true, symbol: 'none', itemStyle: { color: '#0B2118' } },
      { name: '已预约', type: 'line', data: res.data.map(i => i.booked), smooth: true, symbol: 'none', areaStyle: { color: 'rgba(172,244,74,0.1)' }, itemStyle: { color: '#ACF44A' } }
    ]
  })
  charts.push(chart)
}

// 初始化核销状态图
const initCheckinStatus = async () => {
  const res = await getStatsApi.getCheckinStatus()
  if (!checkinStatusChartRef.value) return
  const chart = echarts.init(checkinStatusChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#fff', borderRadius: 8 },
    legend: { 
      orient: 'vertical', 
      left: '5%', 
      top: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#6F767E' }
    },
    series: [{
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['65%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: [
        { value: res.data.checked || 0, name: '已核销' },
        { value: res.data.unchecked || 0, name: '未核销' },
        { value: res.data.cancelled || 0, name: '已取消' }
      ]
    }]
  })
  charts.push(chart)
}

// 初始化资讯排行
const initPopularNews = async () => {
  const res = await getStatsApi.getPopularNews()
  if (!popularNewsChartRef.value) return
  const chart = echarts.init(popularNewsChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '10%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: { 
      type: 'category', 
      data: res.data.map(i => i.title), 
      axisLine: { show: false },
      axisLabel: {
        formatter: (value) => value.length > 10 ? value.substring(0, 10) + '...' : value
      }
    },
    series: [{ name: '阅读量', type: 'bar', data: res.data.map(i => i.viewCnt || 0), itemStyle: { color: '#ACF44A', borderRadius: [0, 10, 10, 0] } }]
  })
  charts.push(chart)
}

// 初始化爽约对比
const initNoShow = async () => {
  const res = await getStatsApi.getNoShowComparison()
  if (!noShowComparisonChartRef.value) return
  const chart = echarts.init(noShowComparisonChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: res.data.map(i => i.label), 
      axisLine: { show: false },
      axisLabel: { interval: 0, fontSize: 11, color: '#6F767E' }
    },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { type: 'dashed' } } },
    series: [{ name: '爽约人数', type: 'bar', data: res.data.map(i => i.value || 0), itemStyle: { color: '#0B2118', borderRadius: [10, 10, 0, 0] } }]
  })
  charts.push(chart)
}

onMounted(() => {
  initAdmin()
  getStatsApi.getHomeStats().then(res => { if (res.code === 200) stats.value = res.data })
  setTimeout(() => {
    initTrend()
    initCheckinStatus()
    initPopularNews()
    initNoShow()
  }, 300)
  window.addEventListener('resize', () => charts.forEach(c => c.resize()))
})
</script>

<style scoped>
.sio-container {
  padding: 40px;
  background-color: #fff;
  min-height: 100vh;
}

/* Update Banner / Admin Card */
.update-banner-card {
  background-color: #0B2118;
  color: #fff;
  border-radius: 24px;
  margin-bottom: 24px;
  border: none;
}
.update-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  text-transform: uppercase;
  font-weight: 700;
  margin-bottom: 24px;
}
.green-dot {
  width: 8px;
  height: 8px;
  background-color: #ACF44A;
  border-radius: 50%;
  box-shadow: 0 0 10px #ACF44A;
}
.admin-profile-mini {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.admin-name {
  font-size: 20px;
  font-weight: 600;
}
.admin-role {
  color: #ACF44A;
  font-size: 12px;
}
.admin-intro-text {
  font-size: 14px;
  color: #6F767E;
  line-height: 1.6;
  margin-bottom: 24px;
}
.update-action {
  font-size: 13px;
  color: #ACF44A;
  cursor: pointer;
  font-weight: 500;
}

/* Statistics */
.dashboard-intro h2 {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 8px;
  color: #1A1D1F;
}
.dashboard-intro p {
  color: #6F767E;
  margin-bottom: 32px;
}
.stat-flat-card {
  height: 120px;
  border-radius: 24px;
  border: 1px solid #F4F4F4 !important;
  margin-bottom: 24px;
  overflow: hidden !important;
}
.flat-header {
  display: flex;
  justify-content: space-between;
  color: #6F767E;
  font-size: 14px;
  margin-bottom: 12px;
}
.flat-value {
  font-size: 36px;
  font-weight: 700;
  color: #1A1D1F;
}
.flat-trend {
  font-size: 12px;
  font-weight: 600;
  margin-top: 8px;
}
.flat-trend.up { color: #00B111; }
.flat-trend.down { color: #FF5E5E; }

/* Charts */
.mt-24 { margin-top: 24px; }
.main-chart-sio {
  height: 380px;
  width: 100%;
}
.mini-chart-pie {
  height: 200px;
}
.side-chart {
  height: 240px;
}
.chart-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.date-picker-fake {
  background: #F7F8FA;
  padding: 8px 16px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #1A1D1F;
}

/* Shortcuts */
.sio-shortcuts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.sio-shortcut-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
}
.sio-shortcut-tile:hover .icon-wrap {
  transform: translateY(-4px);
}
.sio-shortcut-tile span {
  font-size: 12px;
  color: #1A1D1F;
  font-weight: 500;
}
.card-title-mini {
  font-weight: 700;
  color: #1A1D1F;
  margin-bottom: 20px;
  font-size: 16px;
}
</style>