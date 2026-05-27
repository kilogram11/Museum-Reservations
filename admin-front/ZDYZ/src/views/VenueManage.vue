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

    <!-- 场馆编辑/新增表单 -->
    <div v-else class="venue-form">
      <el-card>
        <el-form :model="venueForm" ref="venueRef" label-width="100px">
          <!-- 1. 场馆名称 -->
          <el-form-item label="场馆名称" prop="museumTitle" :rules="[{ required: true, message: '请输入场馆名称', trigger: 'blur' }]">
            <el-input v-model="venueForm.museumTitle" placeholder="请输入场馆名称" class="input-full" />
          </el-form-item>
          
          <!-- 2. 场馆简介 -->
          <el-form-item label="场馆简介" prop="museumDesc">
            <el-input v-model="venueForm.museumDesc" type="textarea" placeholder="请输入场馆简介" class="input-full" />
          </el-form-item>
          
          <!-- 3. 场馆地址 -->
          <el-form-item label="场馆地址" prop="museumAddress">
            <el-input v-model="venueForm.museumAddress" placeholder="请输入场馆地址" class="input-full" />
          </el-form-item>

          <el-form-item label="场馆位置">
           <BaiduMapPicker
             :longitude="venueForm.longitude"
             :latitude="venueForm.latitude"
             @change="onMapChange"
            />
          </el-form-item>

          
          <!-- 4. 联系电话 -->
          <el-form-item label="联系电话" prop="museumPhone">
            <el-input v-model="venueForm.museumPhone" placeholder="请输入联系电话" class="input-full" />
          </el-form-item>
          
          <!-- New: 开放日期范围 (拆分) -->
          <el-form-item label="开放日期" :rules="[{ required: true, message: '请选择完整的开放日期范围', trigger: 'change' }]">
             <el-date-picker
               v-model="venueForm.startDate"
               type="date"
               placeholder="开始日期"
               value-format="YYYY-MM-DD"
               style="width: 48%; margin-right: 2%;"
             />
             <el-date-picker
               v-model="venueForm.endDate"
               type="date"
               placeholder="结束日期"
               value-format="YYYY-MM-DD"
               style="width: 48%;"
             />
          </el-form-item>

          <!-- 5. 可预约提前天数 -->
          <el-form-item label="可预约提前天数" prop="museumBookSet">
            <el-input 
              v-model="venueForm.museumBookSet" 
              type="number" 
              min="1" 
              placeholder="请输入天数" 
              class="input-short" 
            />
          </el-form-item>

          <!-- 6. 参观时段 -->
          <el-form-item label="参观时段">
            <el-table :data="venueForm.times" border style="width: 100%; margin-bottom: 10px;">
              <el-table-column prop="time" label="时段" width="150">
                <template #default="scope">
                  <el-input 
                    v-model="scope.row.time" 
                    size="small"
                    placeholder="如：8:00-10:00" 
                  />
                </template>
              </el-table-column>
              <el-table-column prop="num" label="人数" width="100">
                <template #default="scope">
                  <el-input 
                    v-model="scope.row.num" 
                    type="number" 
                    size="small"
                    min="0"
                    placeholder="可预约人数" 
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="scope">
                  <el-button
                    type="primary"
                    size="small"
                    @click="saveTimeSlotEdit(scope.row, scope.$index)"
                    style="margin-right: 8px;"
                  >
                    保存
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    @click="deleteTimeSlot(scope.$index)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button 
              type="primary" 
              size="small" 
              icon="Plus"
              @click="addNewTimeSlot"
            >
              添加时段
            </el-button>
          </el-form-item>

          <!-- 7. 场馆图片 (多图上传) -->
          <el-form-item label="场馆图片">
            <el-upload
              action="/api/admin/upload/image"
              :headers="uploadHeaders"
              list-type="picture-card"
              :file-list="fileList"
              :on-preview="handlePictureCardPreview"
              :on-remove="handleRemove"
              :on-success="handleUploadSuccess"
              :before-upload="beforeAvatarUpload"
              multiple
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <el-dialog v-model="dialogVisible">
              <img w-full :src="dialogImageUrl" alt="Preview Image" style="width: 100%" />
            </el-dialog>
          </el-form-item>

          <!-- 操作按钮 -->
          <div class="btn-group">
            <el-button @click="cancelEdit">取消</el-button>
            <div class="right-btn">
              <el-button type="primary" @click="handleSubmit(0)" style="margin-left: 10px;">保存</el-button>
              <el-button type="success" @click="handleSubmit(1)" style="margin-left: 10px;">发布</el-button>
            </div>
          </div>
        </el-form>
      </el-card>
    </div>

    <!-- 预览弹窗 -->
    <el-dialog
      title="场馆详情"
      v-model="previewVisible"
      width="60%"
      :append-to-body="true"
    >
      <div class="preview-container">
        <h2 class="preview-title">{{ previewForm.name || '未设置场馆名称' }}</h2>
        <div class="preview-info">
          <p><strong>场馆简介：</strong>{{ previewForm.desc || '无' }}</p>
          <p><strong>场馆地址：</strong>{{ previewForm.address || '无' }}</p>
          <p><strong>联系电话：</strong>{{ previewForm.phone || '无' }}</p>
          <p><strong>可预约提前天数：</strong>{{ previewForm.advanceDays || 0 }}天</p>
        </div>
        <div class="preview-time-slots">
          <h3>参观时段及可预约人数</h3>
          <el-table :data="previewForm.timeSlots || []" border stripe style="width: 100%;">
            <el-table-column prop="time" label="时段" />
            <el-table-column prop="num" label="可预约人数" />
          </el-table>
          <div v-if="(previewForm.timeSlots || []).length === 0" class="empty-time-slots">无参观时段配置</div>
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
// 请根据实际接口路径调整导入
import { 
  getVenueListApi, 
  addVenueApi, 
  editVenueApi, 
  delVenueApi, // 替换deleteVenueApi为delVenueApi
  getVenueDetailApi,
  changeVenueStatusApi 
} from '@/api/venue'
import BaiduMapPicker from '@/components/BaiduMapPicker.vue'

// 基础变量
const router = useRouter()
const { proxy } = getCurrentInstance()
const isEdit = ref(false)
const loading = ref(false)
const venueRef = ref(null)

// 分页相关
const page = ref(1)
const limit = ref(10)
const total = ref(0)

// 预览相关变量
const previewVisible = ref(false)
const previewForm = ref({})

// 图片上传状态
const fileList = ref([])
const dialogImageUrl = ref('')
const dialogVisible = ref(false)

// 上传请求头
const uploadHeaders = {
  Authorization: 'Bearer ' + localStorage.getItem('token') // 假设 token 存在 localStorage
}

// 场馆列表数据
const venueList = ref([])

// 地图选点回调（BaiduMapPicker）
function onMapChange(data) {
  venueForm.longitude = data.longitude
  venueForm.latitude = data.latitude
  venueForm.museumAddress = data.address
}


// 场馆表单
const venueForm = reactive({
  id: '',
  museumTitle: '',
  museumDesc: '',
  museumAddress: '',
  museumPhone: '',
  museumBookSet: 7,
  times: [],
  museumCover: '',
  startDate: '', // 拆分日期
  endDate: '',   // 拆分日期
  museumContent: '', // 隐藏字段:透传富文本内容
  museumStatus: 0,
  museumImgs: [], // 新增: 多图数组
  longitude: 0,  // ← 改为 Number 类型，默认 0
  latitude: 0,   // ← 改为 Number 类型，默认 0
})

// 格式化列表页简介展示
const formatIntro = (row) => {
  return parseObj(row.museumObj).desc || '无简介'
}

// 辅助函数解析 JSON
const parseObj = (jsonStr) => {
  try {
    return jsonStr ? JSON.parse(jsonStr) : {}
  } catch (e) {
    console.error('JSON Parse Error:', e)
    return {}
  }
}

// 解析时段为分钟数（用于排序）
const parseTimeToMinutes = (timeStr) => {
  const [start] = timeStr.split('-')
  if (!start) return 0
  const [hour, minute] = start.split(':').map(Number)
  return hour * 60 + (minute || 0)
}

// 获取场馆列表
const getVenueList = async () => {
  loading.value = true
  try {
    const res = await getVenueListApi.getList({
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
    const res = await getVenueDetailApi.getDetail(row.id)
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
  const newStatus = row.museumStatus === 1 ? 0 : 1 // 替换status为museumStatus
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

    // 修复API方法名：changeStatus 而非 change
    const res = await changeVenueStatusApi.changeStatus({
      id: row.id,
      status: newStatus // 后端接收status字段（对应museumStatus）
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

// 新增一行空的可编辑时段
const addNewTimeSlot = () => {
  venueForm.times.push({ time: '', num: '' })
  ElMessage.info('新增时段行，可直接编辑内容')
}

// 保存单条时段的修改（并排序）
const saveTimeSlotEdit = (row, index) => {
  if (!row.time || !row.time.includes('-')) {
    ElMessage.warning('请输入有效时段格式（如：08:00-10:00）')
    return
  }
  if (!row.num || row.num < 0) {
    ElMessage.warning('请输入有效可预约人数（≥0）')
    return
  }

  // 按开始时间排序
  venueForm.times.sort((a, b) => {
    return parseTimeToMinutes(a.time) - parseTimeToMinutes(b.time)
  })

  ElMessage.success('时段修改保存成功')
}


// 删除时段行
const deleteTimeSlot = (index) => {
  if (venueForm.times.length <= 1) { // 替换timeSlots为times
    ElMessage.warning('至少保留一个时段')
    return
  }
  venueForm.times.splice(index, 1) // 替换timeSlots为times
  ElMessage.success('时段删除成功')
}

// 图片上传成功
const handleUploadSuccess = (res, file, fileListVal) => {
  if (res.code === 200) {
    fileList.value.push({
      name: file.name,
      url: res.data.url
    })
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

// 移除图片
const handleRemove = (file, fileListVal) => {
  const index = fileList.value.findIndex(item => item.url === file.url)
  if (index !== -1) {
    fileList.value.splice(index, 1)
  }
}

// 预览图片
const handlePictureCardPreview = (file) => {
  dialogImageUrl.value = file.url
  dialogVisible.value = true
}

// 图片上传前校验
const beforeAvatarUpload = (file) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('上传图片只能是 JPG/PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 新增场馆
const handleAdd = () => {
  // 默认日期：今天开始，持续一年
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
    times: [{ start: '08:00', end: '10:00', limit: 500 }],
    museumCover: '', 
    startDate: defaultStart, // 设置默认值
    endDate: defaultEnd,     // 设置默认值
    museumContent: '',
    museumStatus: 0,
    museumImgs: [] 
  })
  fileList.value = [] // 重置图片列表
  isEdit.value = true
}

// 编辑场馆
const handleEdit = (row) => {
  const obj = parseObj(row.museumObj)
  // 转换后端 times (start, end, limit) -> 前端 (time, num)
  const times = (obj.times || []).map(t => ({
    time: `${t.start}-${t.end}`,
    num: t.limit
  }))
  
  if (times.length === 0) {
     times.push({ time: '08:00-10:00', num: 500 })
  }

  // 加载日期范围
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
    startDate: startDate, // 直接赋值
    endDate: endDate,     // 直接赋值
    museumContent: obj.content || '', // 关键：保存原有内容
    museumStatus: row.museumStatus || 0,
    museumImgs: [], // 初始化为空，下面填充
    latitude: Number(row.latitude) || 0,   // ← 转为 Number
    longitude: Number(row.longitude) || 0  // ← 转为 Number
  })
  
  console.log('[VenueManage] handleEdit - 加载场馆经纬度:', {
    latitude: venueForm.latitude,
    longitude: venueForm.longitude,
    rawLat: row.latitude,
    rawLng: row.longitude
  })
  
  // 回显图片
  // 尝试解析 museumPic，如果不是 JSON 数组，则回退到 museumCover
  let imgs = []
  try {
     imgs = JSON.parse(row.museumPic)
  } catch (e) {
     if (row.museumCover) imgs = [row.museumCover]
  }

  // 确保是数组
  if (!Array.isArray(imgs)) imgs = []
  
  // 填充 fileList
  fileList.value = imgs.map(url => ({ name: 'img', url: url }))
  
  isEdit.value = true
}

// 取消编辑
const cancelEdit = () => {
  isEdit.value = false
}

// 统一保存/发布逻辑
// status: 0=保存, 1=发布
const handleSubmit = async (status) => {
  console.log('👉 点击提交，status=', status)
  try {
    await venueRef.value.validate()
    console.log('✅ 表单校验通过，准备发请求')
    // 1. 校验时段
    const invalidSlots = venueForm.times.filter(slot => !slot.time || !slot.time.includes('-') || !slot.num || slot.num < 0)
    if (invalidSlots.length > 0) {
      ElMessage.warning('请完善所有时段的内容（时间格式HH:mm-HH:mm，人数≥0）')
      return
    }

    // 2. 构造请求数据
    // 转换前端 times -> 后端
    const backendTimes = venueForm.times.map(t => {
      const [start, end] = t.time.split('-')
      return { start, end, limit: Number(t.num) }
    })
    
    // 获取日期范围
    const startDate = venueForm.startDate
    const endDate = venueForm.endDate

    if (!startDate || !endDate) {
       ElMessage.warning('请选择完整的开放日期范围')
       return
    }

    const reqData = {
      museumTitle: venueForm.museumTitle,
      museumDesc: venueForm.museumDesc,
      museumAddress: venueForm.museumAddress,
      museumPhone: venueForm.museumPhone,
      longitude: venueForm.longitude,
      latitude: venueForm.latitude,
      museumBookSet: venueForm.museumBookSet,
      times: backendTimes,
      // 收集所有图片
      museumImgs: fileList.value.map(f => f.url),
      // 自动设置第一张为封面，兼容旧逻辑
      museumCover: fileList.value.length > 0 ? fileList.value[0].url : '',
      
      // 核心变更：日期和内容透传
      startDate: startDate, 
      endDate: endDate,
      museumContent: venueForm.museumContent, // 必须透传，否则丢失
      
      museumStatus: status, // 0或1
      adminId: localStorage.getItem('adminId'),
      museumMaxJoinCnt: 5000 // 默认值
    }

    console.log('🧪 venueForm.id =', venueForm.id)
    
    let res
    const actionText = status === 1 ? '发布' : '保存'

    if (venueForm.id) {
      // 编辑
      reqData.id = venueForm.id
      res = await editVenueApi.edit(reqData)
    } else {
      // 新增
      res = await addVenueApi.add(reqData)
    }

    if (res.code === 200) {
      ElMessage.success(`${actionText}成功`)
      isEdit.value = false
      getVenueList()
    } else {
      ElMessage.error(res.msg || `${actionText}失败`)
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('表单验证失败，请检查必填项')
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
    const res = await delVenueApi.del({ id: id })
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