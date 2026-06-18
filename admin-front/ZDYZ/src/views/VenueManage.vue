<template>
  <div class="venue-manage-container">
    <el-page-header @back="goBack" content="场馆列表" style="margin-bottom: 20px;" v-if="!isEdit" />
    <el-page-header @back="cancelEdit" content="编辑场馆" style="margin-bottom: 20px;" v-else />

    <!-- 场馆列表展示 -->
    <div v-if="!isEdit" class="venue-list">
      <el-card>
        <div slot="header" class="card-header">
          <span>场馆列表</span>
          <el-button type="primary" size="small" @click="handleAdd">新建</el-button>
        </div>

        <!-- 表格 -->
        <el-table 
          :data="venueList" 
          border 
          stripe 
          :loading="loading"
          @row-click="handleRowClick"
        >
           <el-table-column prop="museumTitle" label="场馆名称" min-width="200" /> <!-- 替换name为museumTitle -->
           <el-table-column 
              label="场馆简介" 
              min-width="300" 
              show-overflow-tooltip
              :formatter="formatIntro"
            >
              <!-- 修复formatter数据源 -->
              <template #default="scope">
                {{ parseObj(scope.row.museumObj).desc || '无简介' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <!-- 替换status为museumStatus -->
                <el-tag :type="scope.row.museumStatus === 1 ? 'success' : 'info'">
                  {{ scope.row.museumStatus === 1 ? '已发布' : '仅保存' }}
                </el-tag>
              </template>
            </el-table-column>
          <el-table-column label="操作" width="280">
            <template #default="scope">
              <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button 
                :type="scope.row.museumStatus === 1 ? 'warning' : 'success'" 
                size="small" 
                @click="handleChangeStatus(scope.row)"
              >
                {{ scope.row.museumStatus === 1 ? '禁用' : '启用' }}
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

    <!-- 场馆表单（子组件） -->
    <VenueForm 
      v-else 
      :form-data="venueForm" 
      :is-edit="isEdit"
      @submit="handleFormSubmit"
      @cancel="cancelEdit"
    />

    <!-- 预览弹窗（子组件） -->
    <VenuePreview 
      :visible="previewVisible"
      :preview-data="previewForm"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, ElTag } from 'element-plus'
import { useRouter } from 'vue-router'
import { venueApi } from '@/api/venue'
import VenueForm from '@/components/VenueForm.vue'
import VenuePreview from '@/components/VenuePreview.vue'

const router = useRouter()
const isEdit = ref(false)
const loading = ref(false)

const page = ref(1)
const limit = ref(10)
const total = ref(0)

const previewVisible = ref(false)
const previewForm = ref({})

const venueList = ref([])

const venueForm = reactive({
  id: '',
  museumTitle: '',
  museumDesc: '',
  museumAddress: '',
  museumPhone: '',
  museumBookSet: 7,
  times: [],
  museumCover: '',
  startDate: '',
  endDate: '',
  museumContent: '',
  museumStatus: 0,
  museumImgs: [],
  longitude: 0,
  latitude: 0,
})

const formatIntro = (row) => {
  return parseObj(row.museumObj).desc || '无简介'
}

const parseObj = (jsonStr) => {
  try {
    return jsonStr ? JSON.parse(jsonStr) : {}
  } catch (e) {
    console.error('JSON Parse Error:', e)
    return {}
  }
}

// 获取场馆列表
const getVenueList = async () => {
  loading.value = true
  try {
    const res = await venueApi.list({
      page: page.value,
      limit: limit.value
    })
    if (res.code === 200) {
      venueList.value = res.data.records
      total.value = res.data.total
    } else {
      ElMessage.error(res.msg || '获取场馆列表失败')
    }
  } catch (err) {
    ElMessage.error('获取场馆列表失败：' + err.message)
  } finally {
    loading.value = false
  }
}

// 分页大小改变
const handleSizeChange = (val) => {
  limit.value = val
  page.value = 1
  getVenueList()
}

// 当前页改变
const handleCurrentChange = (val) => {
  page.value = val
  getVenueList()
}

// 行点击预览
const handleRowClick = async (row) => {
  try {
    // 修复API方法名：getDetail 而非 get
    const res = await venueApi.detail(row.id)
    if (res.code === 200) {
      const detail = res.data
      const obj = parseObj(detail.museumObj)
      previewForm.value = {
        name: detail.museumTitle, 
        desc: obj.desc, 
        address: obj.address, 
        phone: obj.phone || '', 
        advanceDays: detail.museumBookSet,
        // 适配后端排期格式：obj.times数组 -> start/end/limit
        // 修正：从解析后的obj中获取times，而非detail.times
        timeSlots: (obj.times || []).map(item => ({
          time: `${item.start}-${item.end}`,
          num: item.limit
        }))
      }
      previewVisible.value = true
    } else {
      ElMessage.error(res.msg || '获取场馆详情失败')
    }
  } catch (err) {
    ElMessage.error('获取场馆详情失败：' + err.message)
  }
}

// 修改场馆状态
const handleChangeStatus = async (row) => {
  const newStatus = row.museumStatus === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '启用' : '禁用'
  
  try {
    await ElMessageBox.confirm(
      `确定要${statusText}该场馆吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await venueApi.status({
      id: row.id,
      status: newStatus
    })

    if (res.code === 200) {
      ElMessage.success(`${statusText}成功`)
      getVenueList()
    } else {
      ElMessage.error(res.msg || `${statusText}失败`)
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.info(`已取消${statusText}`)
    }
  }
}

// 新增场馆
const handleAdd = () => {
  const today = new Date()
  const nextYear = new Date()
  nextYear.setFullYear(today.getFullYear() + 1)
  
  const defaultStart = today.toISOString().split('T')[0]
  const defaultEnd = nextYear.toISOString().split('T')[0]

  Object.assign(venueForm, {
    id: '',
    museumTitle: '', 
    museumDesc: '', 
    museumAddress: '', 
    museumPhone: '', 
    museumBookSet: 7, 
    times: [{ time: '08:00-10:00', num: 500 }],
    museumCover: '', 
    startDate: defaultStart,
    endDate: defaultEnd,
    museumContent: '',
    museumStatus: 0,
    museumImgs: [],
    longitude: 0,
    latitude: 0
  })
  isEdit.value = true
}

// 编辑场馆
const handleEdit = (row) => {
  const obj = parseObj(row.museumObj)
  const times = (obj.times || []).map(t => ({
    time: `${t.start}-${t.end}`,
    num: t.limit
  }))
  
  if (times.length === 0) {
     times.push({ time: '08:00-10:00', num: 500 })
  }

  const startDate = obj.startDate || ''
  const endDate = obj.endDate || ''

  Object.assign(venueForm, {
    id: row.id,
    museumTitle: row.museumTitle, 
    museumDesc: obj.desc, 
    museumAddress: obj.address, 
    museumPhone: obj.phone || '', 
    museumBookSet: row.museumBookSet || 7,
    times: times,
    museumCover: row.museumCover || '', 
    startDate: startDate,
    endDate: endDate,
    museumContent: obj.content || '',
    museumStatus: row.museumStatus || 0,
    museumImgs: [],
    latitude: Number(row.latitude) || 0,
    longitude: Number(row.longitude) || 0
  })
  
  let imgs = []
  try {
     imgs = JSON.parse(row.museumPic)
  } catch (e) {
     if (row.museumCover) imgs = [row.museumCover]
  }

  if (!Array.isArray(imgs)) imgs = []
  venueForm.museumImgs = imgs
  
  isEdit.value = true
}

// 取消编辑
const cancelEdit = () => {
  isEdit.value = false
}

// 处理表单提交（从子组件接收）
const handleFormSubmit = async (reqData) => {
  const actionText = reqData.museumStatus === 1 ? '发布' : '保存'
  
  let res
  if (venueForm.id) {
    reqData.id = venueForm.id
    res = await venueApi.edit(reqData)
  } else {
    res = await venueApi.add(reqData)
  }

  if (res.code === 200) {
    ElMessage.success(`${actionText}成功`)
    isEdit.value = false
    getVenueList()
  } else {
    ElMessage.error(res.msg || `${actionText}失败`)
  }
}

// 删除场馆
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该场馆吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 修复API方法名+传参格式
    const res = await venueApi.delete(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      getVenueList()
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
  getVenueList()
})
</script>

<style scoped>
.venue-manage-container {
  padding: 20px;
}

/* 列表样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 表单样式 */
.venue-form {
  padding: 0 20px;
}

.input-full {
  width: 100%;
}
.input-short {
  width: 150px;
}

.avatar-uploader {
  width: 150px;
  height: 150px;
  position: relative;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
}
.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-icon {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #999;
}
.text {
  margin-top: 5px;
  font-size: 12px;
}

.btn-group {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  margin-bottom: 20px;
  color: #333;
}
.preview-info {
  line-height: 1.8;
  margin-bottom: 20px;
}
.preview-time-slots h3 {
  margin-bottom: 10px;
  font-size: 16px;
  color: #666;
}
.empty-time-slots {
  text-align: center;
  color: #999;
  padding: 20px;
}

/* 表格行悬停 */
.el-table tr:hover {
  cursor: pointer;
  background-color: #f5f7fa;
}

/* 表格内输入框样式 */
:deep(.el-input--small) {
  --el-input-height: 32px;
  width: 100%;
}

/* 增加表单项间距 */
:deep(.el-form-item) {
  margin-bottom: 25px;
}

/* 增加表格行高，使时段输入框之间不拥挤 */
:deep(.el-table__cell) {
  padding: 20px 0 !important;
}
</style>