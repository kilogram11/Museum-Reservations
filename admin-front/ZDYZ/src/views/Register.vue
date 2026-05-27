<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-left">
        <div class="brand-info">
          <el-icon class="logo-icon"><StarFilled /></el-icon>
          <h1 class="app-name">博物馆预约系统</h1>
        </div>
        <div class="welcome-text">
          <h2>加入管理体系</h2>
          <p>请填写您的身份凭据申请授权接入，密钥由总端分发。</p>
        </div>
        <div class="abstract-shape"></div>
      </div>

      <div class="login-right">
        <div class="form-container">
          <h2 class="form-title">管理员注册</h2>
          <el-form :model="registerForm" ref="registerRef" :rules="registerRules" class="sio-form">
            <el-form-item prop="username">
              <div class="field-label">申请用户名</div>
              <el-input
                v-model="registerForm.username"
                placeholder="建议使用实名拼音"
                class="sio-input"
              />
            </el-form-item>

            <el-form-item prop="password">
              <div class="field-label">登录密码</div>
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="至少6位安全字符"
                show-password
                class="sio-input"
              />
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <div class="field-label">重复确认密码</div>
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次注入密码"
                show-password
                class="sio-input"
              />
            </el-form-item>

            <el-form-item prop="secretKey">
              <div class="field-label">注册授权密钥</div>
              <el-input
                v-model="registerForm.secretKey"
                type="password"
                placeholder="请输入分发密钥"
                class="sio-input"
              />
            </el-form-item>

            <el-button
              type="primary"
              class="sio-login-btn"
              @click="handleRegister"
              :loading="loading"
              style="margin-top: 20px"
            >
              提交审核并注册
            </el-button>

            <div class="auth-switch">
              已有管理权限？ <span @click="$router.push('/admin/auth/login')">直接签署进入</span>
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
import { registerApi } from '@/api/regitser'

const router = useRouter()
const loading = ref(false)
const registerRef = ref(null)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  secretKey: ''
})

const registerRules = reactive({
  username: [
    { required: true, message: '请定义您的身份标识', trigger: 'blur' },
    { min: 3, max: 16, message: '长度需在 3-16 字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请注入安全密码', trigger: 'blur' },
    { min: 6, message: '安全性不足（需大于6位）', trigger: 'blur' }
  ],
  secretKey: [
    { required: true, message: '授权密钥必填', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请重复确认', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('注入的两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

const handleRegister = async () => {
  try {
    await registerRef.value.validate()
    loading.value = true
    const res = await registerApi.register({
      username: registerForm.username,
      password: registerForm.password,
      secretKey: registerForm.secretKey
    })
    if (res.code === 200) {
      ElMessage.success('身份登记完成，请尝试进入系统')
      router.push('/admin/auth/login')
    }
  } catch (err) {
    if (err.msg) ElMessage.error(err.msg)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  registerForm.username = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
  registerForm.secretKey = ''
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
  height: 700px; /* 注册表单较长，调高高度 */
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
  margin-bottom: 32px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #6F767E;
  margin-bottom: 6px;
}

:deep(.sio-input .el-input__wrapper) {
  background-color: #F4F4F4 !important;
  box-shadow: none !important;
  border: 2px solid transparent !important;
  border-radius: 12px !important;
  padding: 4px 16px !important;
  transition: all 0.2s;
}
:deep(.el-form-item) {
  margin-bottom: 18px;
}
:deep(.sio-input .el-input__wrapper.is-focus) {
  border-color: #ACF44A !important;
  background-color: #fff !important;
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
</style>
