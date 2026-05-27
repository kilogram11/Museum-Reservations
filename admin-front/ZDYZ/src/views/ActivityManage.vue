<template>
  <div class="activity-manage-container">
    <el-page-header @back="goBack" content="活动列表" style="margin-bottom: 20px;" v-if="!isEdit" />
    <el-page-header @back="cancelEdit" content="编辑活动" style="margin-bottom: 20px;" v-else />

    <!-- 活动列表/编辑切换 -->
    <div v-if="!isEdit" class="activity-list">
      <el-card>
        <div slot="header" class="card-header">
          <span>活动列表</span>
          <el-button type="primary" size="small" @click="handleAdd">新建</el-button>
        </div>

        <!-- 新增：搜索框 -->
        <div class="search-bar" style="margin-bottom: 15px; display: flex; align-items: center;">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入活动名称关键词搜索"
            style="width: 300px; margin-right: 10px;"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" size="small" @click="handleSearch">搜索</el-button>
          <el-button size="small" @click="resetSearch">重置</el-button>
        </div>

        <!-- 表格 -->
        <el-table 
          :data="activityList" 
          border 
          stripe 
          :loading="loading"
          @row-click="handleRowClick"
        >
          <el-table-column prop="activityTitle" label="活动名称" min-width="200" />
          <el-table-column 
            label="活动简介" 
            min-width="300" 
            show-overflow-tooltip
          >
            <template #default="scope">
              {{ getIntroText(scope.row) }}
            </template>
          </el-table-column>
          <el-table-column label="持续时间" width="200">
             <template #default="scope">
               {{ getDuration(scope.row) }}
             </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.activityStatus === 1 ? 'success' : 'info'">
                {{ scope.row.activityStatus === 1 ? '已发布' : '仅保存' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280"> <!-- 加宽操作列 -->
            <template #default="scope">
              <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
              <!-- 新增：修改状态按钮 -->
              <el-button 
                :type="scope.row.activityStatus === 1 ? 'warning' : 'success'" 
                size="small" 
                @click="handleChangeStatus(scope.row)"
              >
                {{ scope.row.activityStatus === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="page"
          :page-sizes="[5, 10, 20]"
          :page-size="limit"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          style="margin-top: 15px; text-align: right;"
        />

        <el-button style="margin-top: 20px;" @click="goBack">返回</el-button>
      </el-card>
    </div>

    <!-- 活动编辑/新增表单 -->
    <div v-else class="activity-form">
      <el-card>
        <el-form :model="activityForm" ref="activityRef" label-width="100px">
          <!-- 1. 活动名称 -->
          <el-form-item label="活动名称" prop="activityTitle" :rules="[{ required: true, message: '请输入活动名称', trigger: 'blur' }]">
            <el-input v-model="activityForm.activityTitle" placeholder="请输入活动名称" />
          </el-form-item>
          
          <!-- 2. 持续时间 -->
          <el-form-item label="持续时间" :rules="[{ required: true, message: '请选择完整的日期范围', trigger: 'change' }]">
            <el-date-picker
              v-model="activityForm.startDate"
              type="date"
              placeholder="开始日期"
              value-format="YYYY-MM-DD"
              style="width: 48%; margin-right: 2%;"
            />
            <el-date-picker
              v-model="activityForm.endDate"
              type="date"
              placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 48%;"
            />
          </el-form-item>

          <!-- 3. 活动简介 -->
          <el-form-item label="活动简介">
            <div class="intro-blocks">
              <!-- 最顶部的添加面板 -->
              <div v-if="showAddPanelVisible && addPanelIndex === -2" class="add-panel top-add-panel">
                <!-- 文本添加面板 -->
                <el-form-item v-if="addPanelType === 'text'">
                  <el-input
                    v-model="tempContent"
                    type="textarea"
                    :rows="6"
                    placeholder="请输入要添加的文本内容"
                    class="full-width-input"
                    style="margin-bottom: 10px;"
                  />
                  <el-button type="success" size="small" @click="confirmAddBlock">确认添加</el-button>
                  <el-button size="small" style="margin-left: 10px;" @click="cancelAddPanel">取消</el-button>
                </el-form-item>
                <!-- 图片添加面板 -->
                <el-form-item v-if="addPanelType === 'image'">
                  <el-upload
                    class="avatar-uploader"
                    :action="''"  
                    :show-file-list="false"
                    :http-request="handleLocalUpload"  
                    :before-upload="beforeImageUpload"
                  >
                    <div class="upload-icon">
                      <el-icon><Plus /></el-icon>
                      <div class="text">上传图片</div>
                    </div>
                  </el-upload>
                  <el-button size="small" style="margin-left: 10px;" @click="cancelAddPanel">取消</el-button>
                </el-form-item>
              </div>

              <!-- 初始添加按钮 -->
              <div class="initial-actions" style="margin-bottom: 15px;">
                <el-button type="primary" size="small" @click="showAddPanel('text', -2)">添加文本</el-button>
                <el-button type="primary" size="small" style="margin-left: 10px;" @click="showAddPanel('image', -2)">添加图片</el-button>
              </div>

              <!-- 渲染所有活动简介内容 -->
              <div v-for="(block, index) in activityForm.content" :key="index" class="intro-block">
                <!-- 文本区块 -->
                <div v-if="block.type === 'text'" class="text-block">
                  <div class="block-header">
                    <span class="block-label">文本内容</span>
                    <el-button 
                      type="text" 
                      icon="el-icon-delete" 
                      class="delete-btn"
                      @click="deleteBlock(index)"
                    >删除</el-button>
                  </div>
                  <el-input
                    v-model="block.val"
                    type="textarea"
                    :rows="6"
                    placeholder="请输入文本内容"
                    class="full-width-input"
                  />
                </div>
                <!-- 图片区块 -->
                <div v-if="block.type === 'image'" class="image-block">
                  <div class="image-wrapper">
                    <img :src="block.val" class="intro-image" />
                    <el-button 
                      type="text" 
                      icon="el-icon-circle-close" 
                      class="image-delete-btn"
                      @click="deleteBlock(index)"
                    >×</el-button>
                  </div>
                </div>

                <!-- 每个内容下方的添加按钮 -->
                <div class="block-actions" style="margin: 10px 0;">
                  <el-button type="primary" size="small" @click="showAddPanel('text', index)">添加文本</el-button>
                  <el-button type="primary" size="small" style="margin-left: 10px;" @click="showAddPanel('image', index)">添加图片</el-button>
                </div>

                <!-- 精准插入的添加面板 -->
                <div v-if="showAddPanelVisible && addPanelIndex === index" class="add-panel">
                  <!-- 文本添加面板 -->
                  <el-form-item v-if="addPanelType === 'text'">
                    <el-input
                      v-model="tempContent"
                      type="textarea"
                      :rows="6"
                      placeholder="请输入要添加的文本内容"
                      class="full-width-input"
                      style="margin-bottom: 10px;"
                    />
                    <el-button type="success" size="small" @click="confirmAddBlock">确认添加</el-button>
                    <el-button size="small" style="margin-left: 10px;" @click="cancelAddPanel">取消</el-button>
                  </el-form-item>
                  <!-- 图片添加面板 -->
                  <el-form-item v-if="addPanelType === 'image'">
                    <el-upload
                      class="avatar-uploader"
                      :action="''"  
                      :show-file-list="false"
                      :http-request="handleLocalUpload"  
                      :before-upload="beforeImageUpload"
                    >
                      <div class="upload-icon">
                        <el-icon><Plus /></el-icon>
                        <div class="text">上传图片</div>
                      </div>
                    </el-upload>
                    <el-button size="small" style="margin-left: 10px;" @click="cancelAddPanel">取消</el-button>
                  </el-form-item>
                </div>
              </div>
            </div>
          </el-form-item>

          <!-- 操作按钮 -->
          <div class="btn-group">
            <el-button @click="cancelEdit">取消</el-button>
            <div class="right-btn">
              <el-button type="info" @click="showPreview">预览</el-button>
              <el-button type="primary" @click="handleSubmit(0)" style="margin-left: 10px;">保存</el-button>
              <el-button type="success" @click="handleSubmit(1)" style="margin-left: 10px;">发布</el-button>
            </div>
          </div>
        </el-form>
      </el-card>
    </div>

    <!-- 预览弹窗 -->
    <el-dialog
      title="活动详情"
      v-model="previewVisible"
      width="60%"
      :append-to-body="true"
    >
      <div class="preview-container">
        <!-- 预览标题 -->
        <h2 class="preview-title">{{ previewForm.activityTitle || '未设置活动名称' }}</h2>
        
        <!-- 预览活动简介 -->
        <div class="preview-intro">
          <div v-for="(block, index) in previewForm.content" :key="index" class="preview-block">
            <div v-if="block.type === 'text'" class="preview-text">
              {{ block.val }}
            </div>
            <div v-if="block.type === 'image'" class="preview-image">
              <img :src="block.val" alt="活动图片" />
            </div>
          </div>
          <div v-if="previewForm.content.length === 0" class="empty-intro">无活动简介内容</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { ElMessage, ElMessageBox, ElTag } from 'element-plus'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { 
  getActivityListApi, 
  addActivityApi, 
  editActivityApi, 
  deleteActivityApi,
  getActivityDetailApi,
  changeActivityStatusApi // 新增：导入状态修改接口
} from '@/api/activity'
import { uploadImageApi } from '@/api/common' // 导入通用上传API

// 基础变量
const router = useRouter()
const { proxy } = getCurrentInstance()
const isEdit = ref(false)
const loading = ref(false)
const activityRef = ref(null)

// 分页相关
const page = ref(1)
const limit = ref(10)
const total = ref(0)

// 新增：搜索关键词
const searchKeyword = ref('')

// 预览相关变量
const previewVisible = ref(false)
const previewForm = ref({})

// 精准插入控制
const showAddPanelVisible = ref(false)
const addPanelType = ref('')
const addPanelIndex = ref(-1)
const tempContent = ref('')

// 活动列表数据
const activityList = ref([])

// 活动表单
const activityForm = reactive({
  id: '',
  activityTitle: '',
  startDate: '',
  endDate: '',
  status: 0,
  content: []
})

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}.${(d.getMonth() + 1).toString().padStart(2, '0')}.${d.getDate().toString().padStart(2, '0')}`
}

// 辅助函数: 递归解析 JSON (兼容 String 和 Object，处理双重序列化)
const parseObj = (data) => {
  if (!data) return {}
  let res = data
  try {
    for (let i = 0; i < 3; i++) {
        if (typeof res === 'object') return res
        if (typeof res === 'string') {
            const trimmed = res.trim()
            if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
                res = JSON.parse(trimmed)
            } else {
                return {} // 非 JSON 字符串直接返回空对象
            }
        }
    }
  } catch (e) {
    // 仅在开发模式下打印详细错误，防止刷屏
    if (process.env.NODE_ENV === 'development') {
        console.warn('JSON parse warning:', e.message, 'Data:', data)
    }
    return {}
  }
  return typeof res === 'object' ? res : {}
}

// 获取简介文本 (列表页)
const getIntroText = (row) => {
  const obj = parseObj(row.activityObj)
  const content = obj.content || []
  if (!Array.isArray(content) || content.length === 0) return '无简介'
  
  // 过滤无效数据，提取第一个文本
  const firstText = content.find(item => item && item.type === 'text' && item.val)
  return firstText ? firstText.val : (content.some(item => item && item.type === 'image') ? '[图片]' : '无简介')
}

// 获取持续时间 (列表页)
const getDuration = (row) => {
  const obj = parseObj(row.activityObj)
  if (obj.startDate && obj.endDate) {
    return `${obj.startDate} 至 ${obj.endDate}`
  }
  return '未设置'
}

// 获取活动列表
const getActivityList = async () => {
  loading.value = true
  try {
    const res = await getActivityListApi.getList({
      keyword: searchKeyword.value, // 传入搜索关键词
      page: page.value,
      limit: limit.value
    })
    if (res.code === 200) {
      activityList.value = res.data.records
      total.value = res.data.total
    } else {
      ElMessage.error(res.msg || '获取活动列表失败')
    }
  } catch (err) {
    ElMessage.error('获取活动列表失败：' + err.message)
  } finally {
    loading.value = false
  }
}

// 分页大小改变
const handleSizeChange = (val) => {
  limit.value = val
  page.value = 1
  getActivityList()
}

// 当前页改变
const handleCurrentChange = (val) => {
  page.value = val
  getActivityList()
}

// 行点击处理
const handleRowClick = async (row) => {
  try {
    // 修复：接口方法名错误（get 而非 getDetail）
    const res = await getActivityDetailApi.get(row.id)
    if (res.code === 200) {
      const detail = res.data
      const obj = parseObj(detail.activityObj)
      // 组装详情数据
      previewForm.value = {
        activityTitle: detail.activityTitle,
        content: obj.content || []
      }
      previewVisible.value = true
    } else {
      ElMessage.error(res.msg || '获取活动详情失败')
    }
  } catch (err) {
    ElMessage.error('获取活动详情失败：' + err.message)
  }
}

// 新增：搜索功能
const handleSearch = () => {
  page.value = 1 // 搜索后重置页码
  getActivityList()
}

// 新增：重置搜索
const resetSearch = () => {
  searchKeyword.value = ''
  page.value = 1
  getActivityList()
}

// 新增：修改活动状态
const handleChangeStatus = async (row) => {
  const newStatus = row.activityStatus === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '启用' : '禁用'
  
  try {
    await ElMessageBox.confirm(
      `确定要${statusText}该活动吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await changeActivityStatusApi.change({
      id: row.id,
      status: newStatus
    })

    if (res.code === 200) {
      ElMessage.success(`${statusText}成功`)
      getActivityList() // 刷新列表
    } else {
      ElMessage.error(res.msg || `${statusText}失败`)
    }
  } catch (err) {
    if (err !== 'cancel') { // 排除取消操作的报错
      ElMessage.info(`已取消${statusText}`)
    }
  }
}

// 显示添加面板
const showAddPanel = (type, index) => {
  addPanelType.value = type
  addPanelIndex.value = index
  showAddPanelVisible.value = true
  tempContent.value = ''
}

// 取消添加面板
const cancelAddPanel = () => {
  showAddPanelVisible.value = false
  addPanelType.value = ''
  addPanelIndex.value = -1
  tempContent.value = ''
}

// 确认添加区块
const confirmAddBlock = () => {
  if (addPanelType.value === 'text' && !tempContent.value.trim()) {
    ElMessage.warning('请输入文本内容')
    return
  }

  // 文本处理
  if (addPanelType.value === 'text') {
    const newText = tempContent.value.trim()
    
    if (addPanelIndex.value === -2) {
      // 顶部添加
      if (activityForm.content.length === 0) {
        activityForm.content.unshift({ type: 'text', val: newText })
      } else if (activityForm.content[0].type === 'text') {
        activityForm.content[0].val = `${newText}\n${activityForm.content[0].val}`
      } else {
        activityForm.content.unshift({ type: 'text', val: newText })
      }
    } else {
      // 精准插入
      const targetIndex = addPanelIndex.value
      if (activityForm.content[targetIndex].type === 'text') {
        activityForm.content[targetIndex].val += `\n${newText}`
      } else {
        const nextBlockIndex = targetIndex + 1
        if (
          nextBlockIndex < activityForm.content.length && 
          activityForm.content[nextBlockIndex].type === 'text'
        ) {
          activityForm.content[nextBlockIndex].val = `${newText}\n${activityForm.content[nextBlockIndex].val}`
        } else {
          activityForm.content.splice(nextBlockIndex, 0, {
            type: 'text',
            val: newText
          })
        }
      }
    }
  }
  
  cancelAddPanel()
  ElMessage.success('添加成功')
}

// 图片上传处理（真实上传）
const handleLocalUpload = async (options) => {
  const file = options.file
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await uploadImageApi.upload(formData)
    if (res.code === 200) {
      const imgUrl = res.data.url
      
      if (addPanelIndex.value === -2) {
        // 顶部添加图片
        activityForm.content.unshift({ type: 'image', val: imgUrl })
      } else {
        // 精准插入图片
        const targetIndex = addPanelIndex.value
        activityForm.content.splice(targetIndex + 1, 0, {
          type: 'image',
          val: imgUrl
        })
      }
      
      cancelAddPanel()
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error(res.msg || '图片上传失败')
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('图片上传异常')
  }
}

// 图片上传前校验
const beforeImageUpload = (file) => {
  const isImg = file.type.includes('image')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImg) {
    ElMessage.error('请上传图片格式文件！')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB！')
    return false
  }
  return true
}

// 删除区块
const deleteBlock = (index) => {
  ElMessageBox.confirm(
    '确定要删除该内容吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    activityForm.content.splice(index, 1)
    ElMessage.success('删除成功')
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 预览功能
const showPreview = async () => {
  if (activityForm.id) {
    // 已有活动，调用详情接口
    try {
      // 修复：接口方法名错误
      const res = await getActivityDetailApi.get(activityForm.id)
      if (res.code === 200) {
        const detail = res.data
        // 修正：需解析 activityObj 获取 content
        const obj = parseObj(detail.activityObj)
        previewForm.value = {
          activityTitle: detail.activityTitle,
          content: obj.content || []
        }
        previewVisible.value = true
      } else {
        ElMessage.error(res.msg || '获取活动详情失败')
      }
    } catch (err) {
      ElMessage.error('获取活动详情失败：' + err.message)
    }
  } else {
    // 新增活动，直接使用表单数据
    previewForm.value = {
      activityTitle: activityForm.activityTitle,
      content: JSON.parse(JSON.stringify(activityForm.content || []))
    }
    previewVisible.value = true
  }
}

// 新增活动
const handleAdd = () => {
  // 重置表单
  Object.assign(activityForm, {
    id: '',
    activityTitle: '',
    startDate: '',
    endDate: '',
    status: 0,
    content: []
  })
  isEdit.value = true
}

// 编辑活动
const handleEdit = (row) => {
  const obj = parseObj(row.activityObj)
  
  // 赋值表单
  Object.assign(activityForm, {
    id: row.id,
    activityTitle: row.activityTitle,
    startDate: obj.startDate || '',
    endDate: obj.endDate || '',
    status: row.activityStatus,
    content: obj.content || []
  })
  isEdit.value = true
}

// 取消编辑
const cancelEdit = () => {
  isEdit.value = false
}

// 统一保存/发布逻辑
const handleSubmit = async (status) => {
  try {
    await activityRef.value.validate()
    
    if (!activityForm.startDate || !activityForm.endDate) {
      ElMessage.warning('请选择完整的日期范围')
      return
    }

    // 构造请求数据
    const reqData = {
      activityTitle: activityForm.activityTitle,
      startDate: activityForm.startDate, // 已通过 value-format 格式化
      endDate: activityForm.endDate,     // 已通过 value-format 格式化
      status: status, // 0或1
      content: activityForm.content,
      adminId: localStorage.getItem('adminId')
    }

    let res
    const actionText = status === 1 ? '发布' : '保存'

    if (activityForm.id) {
      // 编辑
      reqData.id = activityForm.id
      res = await editActivityApi.edit(reqData)
    } else {
      // 新增
      res = await addActivityApi.add(reqData)
    }

    if (res.code === 200) {
      ElMessage.success(`${actionText}成功`)
      isEdit.value = false
      getActivityList()
    } else {
      ElMessage.error(res.msg || `${actionText}失败`)
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('表单验证失败，请检查必填项')
  }
}

// 删除活动
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该活动吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await deleteActivityApi.delete(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      getActivityList()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (err) {
    ElMessage.info('已取消删除')
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 初始化加载列表
onMounted(() => {
  getActivityList()
})
</script>

<style scoped>
.activity-manage-container {
  padding: 20px;
}

/* 列表样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 新增：搜索栏样式 */
.search-bar {
  padding: 0 0 10px 0;
}

/* 表单样式 */
.activity-form {
  padding: 0 20px;
}

.full-width-input {
  width: 100%;
}

.intro-blocks {
  width:100%;
  border: 1px solid #e6e6e6;
  padding: 15px;
  border-radius: 4px;
  background-color: #f9f9f9;
}

.intro-block {
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #e6e6e6;
}

.text-block {
  margin-bottom: 10px;
}

.block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.block-label {
  font-weight: bold;
  color: #666;
}

.delete-btn {
  color: #f56c6c;
}

.image-block {
  margin-bottom: 10px;
}

.image-wrapper {
  position: relative;
  display: inline-block;
}

.intro-image {
  max-width: 300px;
  max-height: 200px;
  border-radius: 4px;
}

.image-delete-btn {
  position: absolute;
  top: -10px;
  right: -10px;
  background-color: #fff;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  padding: 0;
  color: #f56c6c;
  font-size: 16px;
}

.add-panel {
  margin: 10px 0;
  padding: 10px;
  border: 1px dashed #409eff;
  border-radius: 4px;
  background-color: #f0f7ff;
}

.top-add-panel {
  margin-bottom: 15px;
}

.upload-icon {
  width: 100px;
  height: 100px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.btn-group {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
}

.right-btn {
  display: flex;
  align-items: center;
}

/* 预览样式 */
.preview-container {
  padding: 10px;
}

.preview-title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 10px;
  color: #333;
}

.preview-time {
  text-align: center;
  color: #666;
  margin-bottom: 10px;
}

.time-separator {
  margin: 0 10px;
}

.preview-status {
  text-align: center;
}

.preview-intro {
  line-height: 1.6;
}

.preview-block {
  margin-bottom: 20px;
}

.preview-text {
  white-space: pre-wrap;
  color: #333;
}

.preview-image {
  text-align: center;
}

.preview-image img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 4px;
}

.empty-intro {
  text-align: center;
  color: #999;
  padding: 20px;
}

/* 表格行悬停 */
.el-table tr:hover {
  cursor: pointer;
  background-color: #f5f7fa;
}
</style>