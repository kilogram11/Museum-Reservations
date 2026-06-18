<template>
  <div class="venue-form">
    <el-card>
      <el-form :model="localForm" ref="venueRef" label-width="100px">
        <el-form-item label="场馆名称" prop="museumTitle" :rules="[{ required: true, message: '请输入场馆名称', trigger: 'blur' }]">
          <el-input v-model="localForm.museumTitle" placeholder="请输入场馆名称" class="input-full" />
        </el-form-item>
        
        <el-form-item label="场馆简介" prop="museumDesc">
          <el-input v-model="localForm.museumDesc" type="textarea" placeholder="请输入场馆简介" class="input-full" />
        </el-form-item>
        
        <el-form-item label="场馆地址" prop="museumAddress">
          <el-input v-model="localForm.museumAddress" placeholder="请输入场馆地址" class="input-full" />
        </el-form-item>

        <el-form-item label="场馆位置">
         <BaiduMapPicker
           :longitude="localForm.longitude"
           :latitude="localForm.latitude"
           @change="onMapChange"
          />
        </el-form-item>

        <el-form-item label="联系电话" prop="museumPhone">
          <el-input v-model="localForm.museumPhone" placeholder="请输入联系电话" class="input-full" />
        </el-form-item>
        
        <el-form-item label="开放日期" :rules="[{ required: true, message: '请选择完整的开放日期范围', trigger: 'change' }]">
           <el-date-picker
             v-model="localForm.startDate"
             type="date"
             placeholder="开始日期"
             value-format="YYYY-MM-DD"
             style="width: 48%; margin-right: 2%;"
           />
           <el-date-picker
             v-model="localForm.endDate"
             type="date"
             placeholder="结束日期"
             value-format="YYYY-MM-DD"
             style="width: 48%;"
           />
        </el-form-item>

        <el-form-item label="可预约提前天数" prop="museumBookSet">
          <el-input 
            v-model="localForm.museumBookSet" 
            type="number" 
            min="1" 
            placeholder="请输入天数" 
            class="input-short" 
          />
        </el-form-item>

        <el-form-item label="参观时段">
          <el-table :data="localForm.times" border style="width: 100%; margin-bottom: 10px;">
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

        <div class="btn-group">
          <el-button @click="handleCancel">取消</el-button>
          <div class="right-btn">
            <el-button type="primary" @click="handleSubmit(0)" style="margin-left: 10px;">保存</el-button>
            <el-button type="success" @click="handleSubmit(1)" style="margin-left: 10px;">发布</el-button>
          </div>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import BaiduMapPicker from './BaiduMapPicker.vue'

const props = defineProps({
  formData: {
    type: Object,
    required: true
  },
  isEdit: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['submit', 'cancel'])

const venueRef = ref(null)
const fileList = ref([])
const dialogImageUrl = ref('')
const dialogVisible = ref(false)

const uploadHeaders = {
  Authorization: 'Bearer ' + localStorage.getItem('token')
}

const localForm = reactive({
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

watch(() => props.formData, (newData) => {
  Object.assign(localForm, newData)
  if (newData.museumImgs && Array.isArray(newData.museumImgs)) {
    fileList.value = newData.museumImgs.map((img, index) => ({
      name: `图片${index + 1}`,
      url: img,
      uid: index
    }))
  } else {
    fileList.value = []
  }
}, { immediate: true, deep: true })

function onMapChange(data) {
  localForm.longitude = data.longitude
  localForm.latitude = data.latitude
  localForm.museumAddress = data.address
}

const addNewTimeSlot = () => {
  localForm.times.push({
    time: '',
    num: ''
  })
}

const deleteTimeSlot = (index) => {
  localForm.times.splice(index, 1)
}

const saveTimeSlotEdit = (row, index) => {
  if (!row.time || !row.num) {
    ElMessage.warning('请填写完整的时段信息')
    return
  }
  localForm.times[index] = { ...row }
  ElMessage.success('保存成功')
}

const handlePictureCardPreview = (file) => {
  dialogImageUrl.value = file.url
  dialogVisible.value = true
}

const handleRemove = (file, fileList) => {
  const index = localForm.museumImgs.indexOf(file.url)
  if (index > -1) {
    localForm.museumImgs.splice(index, 1)
  }
}

const handleUploadSuccess = (response, file) => {
  if (response.code === 200) {
    localForm.museumImgs.push(response.data)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片格式')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过2MB')
    return false
  }
  return true
}

const handleCancel = () => {
  emit('cancel')
}

const handleSubmit = (status) => {
  venueRef.value.validate((valid) => {
    if (!valid) {
      ElMessage.error('请检查必填项')
      return
    }

    const reqData = {
      ...localForm,
      museumStatus: status,
      museumObj: JSON.stringify({
        desc: localForm.museumDesc,
        address: localForm.museumAddress,
        phone: localForm.museumPhone,
        times: localForm.times.map(item => ({
          start: item.time.split('-')[0],
          end: item.time.split('-')[1] || '',
          limit: Number(item.num) || 0
        }))
      })
    }

    emit('submit', reqData)
  })
}
</script>