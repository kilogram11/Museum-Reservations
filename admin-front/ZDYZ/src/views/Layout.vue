<template>
  <el-container style="height: 100vh;">
    <!-- 左侧导航 -->
    <el-aside width="240px" class="sio-aside">
      <div class="sidebar-header">
        <el-icon class="sio-logo-icon" color="#ACF44A"><StarFilled /></el-icon>
        <span class="sio-app-name">场馆预约系统</span>
      </div>
      <div class="menu-section-label">管理菜单</div>
      <el-menu
        :default-active="$route.path"
        class="sio-menu"
        :router="true"
      >
        <el-menu-item index="/admin/home">
          <el-icon><HomeFilled /></el-icon>
          <span>首页总览</span>
        </el-menu-item>
        <el-menu-item index="/admin/join">
          <el-icon><Check /></el-icon>
          <span>预约核销</span>
        </el-menu-item>
        <el-menu-item index="/admin/museum-config">
          <el-icon><Setting /></el-icon>
          <span>场馆配置</span>
        </el-menu-item>
        <el-menu-item index="/admin/blacklist">
          <el-icon><UserFilled /></el-icon>
          <span>黑名单管理</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-spacer"></div>

      <div class="sidebar-footer">
        <div class="user-info-capsule" @click="showAvatarPopup = !showAvatarPopup">
          <el-avatar :size="32" :src="currentAvatar" />
          <div class="user-text">
            <div class="user-name">{{ userName }}</div>
            <div class="user-email">admin@museum.com</div>
          </div>
        </div>
      </div>

      <!-- 个人信息修改悬浮窗 (侧边栏版本) -->
      <Transition name="fade">
        <div class="sio-popup-sidebar" v-if="showAvatarPopup">
          <div class="popup-header-sio">个人设置</div>
          
          <!-- 用户名编辑 -->
          <div class="popup-item-sio-edit">
            <div class="edit-label">用户名</div>
            <el-input v-model="userName" size="small" @blur="handleUsernameBlur" placeholder="请输入用户名">
              <template #suffix><el-icon><Edit /></el-icon></template>
            </el-input>
          </div>

          <!-- 简介编辑 -->
          <div class="popup-item-sio-edit">
            <div class="edit-label">个人简介</div>
            <el-input 
              v-model="userIntro" 
              type="textarea" 
              :rows="2" 
              size="small" 
              @blur="handleIntroBlur" 
              placeholder="请输入简介"
              resize="none"
            />
          </div>

          <!-- 头像选择 -->
          <div class="popup-item-sio-edit">
            <div class="edit-label">选择头像</div>
            <div class="avatar-mini-grid">
              <div 
                v-for="avatar in avatarList" 
                :key="avatar" 
                class="avatar-mini-item"
                :class="{ active: currentAvatar === avatar }"
                @click="changeAvatar(avatar)"
              >
                <img :src="avatar" />
              </div>
            </div>
          </div>

          <div class="popup-divider"></div>
          <div class="popup-item-sio danger" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon> <span>退出登录</span>
          </div>
        </div>
      </Transition>
    </el-aside>
    <!-- 右侧内容 -->
    <el-container class="sio-main-container">
      <el-header class="sio-header">
        <div class="header-breadcrumb">{{ $route.meta.title || '首页' }} <el-icon><ArrowDown /></el-icon></div>
        <div class="header-actions">
           <span class="now-time-display">{{ nowTime }}</span>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
// 新增：导入编辑图标
import { 
  HomeFilled, Check, Setting, UserFilled, SwitchButton, Edit, 
  StarFilled, Search, Bell, Files, Plus, ArrowDown 
} from '@element-plus/icons-vue'
// 导入 venue API 获取场馆信息
import { getAllVenueApi } from '@/api/venue'
// 新增：导入个人信息接口
import { profileApi } from '@/api/profile'
// 新增：导入4个固定头像（需放在 assets/avatars 目录下，可替换为实际图片路径）
import avatar1 from '@/assets/avatars/1.jpg'
import avatar2 from '@/assets/avatars/2.jpg'
import avatar3 from '@/assets/avatars/3.jpg'
import avatar4 from '@/assets/avatars/4.jpg'

const router = useRouter()
const userName = ref('')
const nowTime = ref('')
const museumName = ref('A博物馆') // 默认值

// 新增：用户头像相关状态
const avatarList = ref([avatar1, avatar2, avatar3, avatar4]) // 4个固定头像选项
const currentAvatar = ref(avatar1) // 初始头像（默认第一个）
const showAvatarPopup = ref(false) // 头像弹窗显示状态
const editUsername = ref(false) // 用户名编辑状态
const editIntro = ref(false) // 用户简介编辑状态
const userIntro = ref('管理员，负责场馆预约系统管理') // 初始简介

// 新增响应式变量存储输入框高度
const introInputRef = ref(null)
const introInputHeight = ref(32)  // 初始高度（与输入框默认高度一致）

// 格式化时间
const formatTime = () => {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  nowTime.value = `${year}年${month}月${day}日 ${hour}:${minute}:${second}`
}

// 获取场馆名称
const getMuseumInfo = async () => {
  try {
    const res = await getAllVenueApi.getAll()
    if (res.data && res.data.length > 0) {
      // 默认取第一个场馆作为当前展示
      museumName.value = res.data[0].museumTitle
    }
  } catch (e) {
    console.error('获取场馆信息失败', e)
  }
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定退出登录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    localStorage.removeItem('token')
    ElMessage.success('退出成功')
    router.push('/admin/auth/login')
  })
}

// 新增：切换头像方法
const changeAvatar = async (avatar) => {
  try {
    // 调用后端接口保存头像
    await profileApi.updateAvatar({ avatarUrl: avatar })
    currentAvatar.value = avatar
    // 同步到本地存储
    localStorage.setItem('userAvatar', avatar)
    ElMessage.success('头像修改成功')
  } catch (e) {
    console.error('头像修改失败', e)
    ElMessage.error('头像修改失败，请重试')
  }
}

// 处理用户名编辑失焦
const handleUsernameBlur = async () => {
  editUsername.value = false
  // 调用后端保存用户名
  await saveUserInfoToServer()
}

// 处理简介编辑失焦
const handleIntroBlur = async () => {
  editIntro.value = false
  // 调用后端保存简介
  await saveUserInfoToServer()
}

// 保存用户名/简介到后端
const saveUserInfoToServer = async () => {
  try {
    await profileApi.updateProfile({
      userName: userName.value,
      userIntro: userIntro.value
    })
    ElMessage.success('信息修改成功')
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存失败，请重试')
  }
}

// 新增：调整输入框高度的核心方法
const adjustIntroInputHeight = () => {
  if (!introInputRef.value) return
  
  // 获取文本域DOM元素
  const textareaEl = introInputRef.value.$el.querySelector('textarea')
  if (!textareaEl) return

  // 关键：先重置高度为auto，让浏览器计算真实滚动高度
  textareaEl.style.height = 'auto'
  
  // 计算内容总高度（scrollHeight包含所有行的高度，不含滚轮）
  // 加8px补偿内边距差异，确保无裁剪
  const contentHeight = textareaEl.scrollHeight + 8
  
  // 设置最小高度（32px）和最大高度（避免过高超出弹窗，可按需调整）
  const minHeight = 32
  const maxHeight = 150  // 最大显示约5-6行，可根据需求修改
  
  // 最终高度 = 内容高度（不超过最大高度，不低于最小高度）
  const finalHeight = Math.min(Math.max(contentHeight, minHeight), maxHeight)
  introInputHeight.value = finalHeight
  
  // 同步设置文本域高度
  textareaEl.style.height = `${finalHeight}px`
}

// 初始化个人信息（从后端获取）
const initAdminProfile = async () => {
  try {
    const res = await profileApi.getProfile()
    if (res.data) {
      // 赋值后端返回的个人信息
      userName.value = res.data.userName
      userIntro.value = res.data.userIntro
      currentAvatar.value = res.data.currentAvatar
      // 同步到本地存储
      localStorage.setItem('userAvatar', res.data.currentAvatar)
    }
  } catch (e) {
    console.error('初始化个人信息失败', e)
    // 本地存储兜底
    const savedAvatar = localStorage.getItem('userAvatar')
    if (savedAvatar) {
      currentAvatar.value = savedAvatar
    }
  }
}

// 监听编辑状态激活，初始化高度
watch(editIntro, (isActive) => {
  if (isActive) {
    // 延迟执行，确保输入框已渲染
    setTimeout(() => {
      adjustIntroInputHeight()
    }, 0)
  } else {
    // 退出编辑时重置高度，下次激活重新计算
    introInputHeight.value = 32
  }
})

// 监听用户简介内容变化（如初始化、后端同步后），调整高度
watch(userIntro, () => {
  if (editIntro.value) {
    adjustIntroInputHeight()
  }
})

// onMounted中新增调用
onMounted(() => {
  formatTime();
  setInterval(formatTime, 1000);
  getMuseumInfo();
  initAdminProfile(); // 初始化个人信息
  // 本地存储兜底（避免接口失败时无头像）
  const savedAvatar = localStorage.getItem('userAvatar');
  if (savedAvatar) currentAvatar.value = savedAvatar;
})
</script>
<style scoped>
.sio-aside {
  background-color: #0B2118;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px 32px;
}
.sio-logo-icon {
  font-size: 28px;
}
.sio-app-name {
  color: #fff;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.5px;
}
.menu-section-label {
  color: #6F767E;
  font-size: 11px;
  font-weight: 700;
  padding: 0 16px 12px;
  letter-spacing: 1px;
}
.sio-menu {
  background-color: transparent !important;
  border: none !important;
}
:deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  border-radius: 12px;
  margin-bottom: 4px;
  color: #6F767E !important;
  padding: 0 16px !important;
  transition: all 0.2s;
}
:deep(.el-menu-item .el-icon) {
  color: #6F767E;
  font-size: 20px;
}
:deep(.el-menu-item.is-active) {
  background-color: #152A22 !important;
  color: #ACF44A !important;
}
:deep(.el-menu-item.is-active .el-icon) {
  color: #ACF44A;
}
:deep(.el-menu-item:hover) {
  background-color: rgba(255,255,255,0.05) !important;
}

.sio-header {
  height: 80px !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  background-color: #fff;
}
.header-breadcrumb {
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}
.search-box-placeholder {
  background-color: #F7F8FA;
  padding: 10px 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #9A9FA5;
  width: 280px;
}
.header-icon {
  font-size: 20px;
  color: #1A1D1F;
  cursor: pointer;
}
.add-btn-fake {
  background-color: #1A1D1F;
  color: #fff;
  padding: 10px 16px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}
.sidebar-spacer {
  flex: 1;
}
.sidebar-footer {
  padding: 16px;
}
.user-info-capsule {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}
.user-text {
  flex: 1;
  overflow: hidden;
}
.user-name {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
}
.user-email {
  color: #6F767E;
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sio-popup-sidebar {
  position: absolute;
  bottom: 80px;
  left: 16px;
  width: 200px; /* 调整为更精致的宽度，约 4:3 比例感 */
  background-color: #1A2D24;
  border-radius: 20px;
  padding: 16px;
  z-index: 200;
  box-shadow: 0 10px 40px rgba(0,0,0,0.5);
  border: 1px solid rgba(255,255,255,0.1);
}
.popup-header-sio {
  color: #ACF44A;
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 20px;
}
.popup-item-sio-edit {
  margin-bottom: 16px;
}
.edit-label {
  color: #6F767E;
  font-size: 12px;
  margin-bottom: 8px;
}
.avatar-mini-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.avatar-mini-item {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
}
.avatar-mini-item.active {
  border-color: #ACF44A;
}
.avatar-mini-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.popup-divider {
  height: 1px;
  background: rgba(255,255,255,0.1);
  margin: 16px 0;
}
.popup-item-sio {
  color: #fff;
  padding: 4px 0;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-size: 14px;
}
.popup-item-sio.danger {
  color: #FF5E5E;
}
.sio-popup-sidebar :deep(.el-textarea__inner), 
.sio-popup-sidebar :deep(.el-input__wrapper) {
  background-color: #FFFFFF !important;
  border-radius: 10px !important;
  border: 1px solid #E0E0E0 !important;
  box-shadow: none !important; /* 移除默认阴影/边框 */
}
.sio-popup-sidebar :deep(.el-input__inner) {
  color: #1A1D1F !important;
  background: transparent !important;
}
.now-time-display {
  color: #6F767E;
  font-size: 13px;
}
</style>
