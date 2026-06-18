<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-left">
        <div class="brand-info">
          <el-icon class="logo-icon"><StarFilled /></el-icon>
          <h1 class="app-name">博物馆预约系统</h1>
        </div>
        <div class="welcome-text">
          <h2>系统管理门户</h2>
          <p>欢迎回来，请使用您的凭据访问控制台。</p>
        </div>
        <div class="abstract-shape"></div>
      </div>

      <div class="login-right">
        <div class="form-container">
          <h2 class="form-title">身份验证</h2>
          <el-form :model="loginForm" ref="loginRef" :rules="loginRules" class="sio-form">
            <el-form-item prop="username">
              <div class="field-label">管理员账号</div>
              <el-input
                v-model="loginForm.username"
                placeholder="请输入账号"
                class="sio-input"
              />
            </el-form-item>

            <el-form-item prop="password">
              <div class="field-label">安全密码</div>
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
                class="sio-input"
              />
            </el-form-item>

            <div class="form-options">
               <el-checkbox v-model="loginForm.remember" label="记住账号" />
               <span class="forget-link">忘记密码？</span>
            </div>

            <el-button
              type="primary"
              class="sio-login-btn"
              @click="handleLogin"
              :loading="loading"
            >
              签署进入
            </el-button>

            <div class="auth-switch">
              没有权限权限？ <span @click="$router.push('/admin/auth/register')">联系注册申请</span>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { StarFilled } from '@element-plus/icons-vue'
import { loginApi } from '@/api/user'

const router = useRouter()
const loading = ref(false)
const loginRef = ref(null)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const loginRules = reactive({
  username: [{ required: true, message: '身份凭据必填', trigger: 'blur' }],
  password: [{ required: true, message: '安全密钥必填', trigger: 'blur' }]
})

const handleLogin = async () => {
  try {
    await loginRef.value.validate()
    loading.value = true
    const res = await loginApi.login(loginForm)
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('adminId', res.data.adminId)
      ElMessage.success('授权接入成功')
      router.push('/admin/home')
    }
  } catch (err) {
    if (err.msg) ElMessage.error(err.msg)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loginForm.username = ''
  loginForm.password = ''
})
</script>

<style scoped>
.login-page {
  width: 100vw;
  height: 100vh;
  background-color: #05140F;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-card {
  width: 1000px;
  height: 600px;
  background-color: #0B2118;
  border-radius: 40px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 40px 100px rgba(0,0,0,0.5);
}

.login-left {
  flex: 1;
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  background: linear-gradient(135deg, #0B2118 0%, #05140F 100%);
  border-right: 1px solid rgba(172,244,74,0.05);
}

.brand-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo-icon {
  font-size: 32px;
  color: #ACF44A;
}
.app-name {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -1px;
}

.welcome-text h2 {
  font-size: 48px;
  color: #fff;
  margin: 0 0 16px;
  line-height: 1.1;
}
.welcome-text p {
  color: #6F767E;
  font-size: 16px;
  max-width: 300px;
}

.abstract-shape {
  position: absolute;
  bottom: -50px;
  right: -50px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(172,244,74,0.15) 0%, transparent 70%);
  filter: blur(40px);
}

.login-right {
  flex: 1;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-container {
  width: 100%;
  max-width: 340px;
}
.form-title {
  font-size: 32px;
  font-weight: 700;
  color: #1A1D1F;
  margin-bottom: 40px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #6F767E;
  margin-bottom: 8px;
}

:deep(.sio-input .el-input__wrapper) {
  background-color: #F4F4F4 !important;
  box-shadow: none !important;
  border: 2px solid transparent !important;
  border-radius: 16px !important;
  padding: 8px 16px !important;
  transition: all 0.2s;
}
:deep(.sio-input .el-input__wrapper.is-focus) {
  border-color: #ACF44A !important;
  background-color: #fff !important;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}
.forget-link {
  font-size: 13px;
  color: #6F767E;
  cursor: pointer;
}

.sio-login-btn {
  width: 100%;
  height: 56px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: 700;
  background-color: #0B2118 !important;
  border: none !important;
  transition: transform 0.2s;
}
.sio-login-btn:hover {
  transform: translateY(-2px);
  background-color: #152A22 !important;
}

.auth-switch {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #6F767E;
}
.auth-switch span {
  color: #0B2118;
  font-weight: 700;
  cursor: pointer;
  margin-left: 4px;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #ACF44A !important;
  border-color: #ACF44A !important;
}
:deep(.el-checkbox__label) {
  color: #6F767E;
}
</style>
