<template>
  <div class="message-template-container">
    <el-page-header @back="goBack" content="消息模版管理" style="margin-bottom: 20px;" />

    <el-card>
      <div slot="header" class="card-header">
        <span>消息模版列表</span>
      </div>

      <el-table 
        :data="templateList" 
        border 
        stripe 
        :loading="loading"
      >
        <el-table-column prop="code" label="模版编码" width="180" />
        <el-table-column prop="titleTemplate" label="标题模版" width="200" />
        <el-table-column prop="contentTemplate" label="内容模版" min-width="300" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="180" :formatter="formatTime" />
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog 
      title="编辑消息模版" 
      v-model="editDialogVisible" 
      width="500px"
    >
      <el-form :model="editForm" ref="formRef" label-width="100px">
        <el-form-item label="模版编码">
          <el-input v-model="editForm.code" disabled />
        </el-form-item>
        <el-form-item label="标题模版" prop="titleTemplate">
          <el-input v-model="editForm.titleTemplate" />
        </el-form-item>
        <el-form-item label="内容模版" prop="contentTemplate">
          <el-input
            v-model="editForm.contentTemplate"
            type="textarea"
            :rows="4"
          />
          <div class="tip">参数说明：{0}, {1} 为动态参数占位符，请勿随意修改顺序。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router' // Import router
import { ElMessage } from 'element-plus'
import { getTemplateListApi, updateTemplateApi } from '@/api/message'

const router = useRouter() // Use router
const loading = ref(false)
const templateList = ref([])
const editDialogVisible = ref(false)
const formRef = ref(null)

const editForm = reactive({
  id: '',
  code: '',
  titleTemplate: '',
  contentTemplate: ''
})

// 时间格式化
const formatTime = (row, column, cellValue) => {
  if (!cellValue) return ''
  const date = new Date(cellValue)
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

const getList = async () => {
  try {
    loading.value = true
    const res = await getTemplateListApi.getList({ page: 1, limit: 100 })
    templateList.value = res.data.records || res.data
  } catch (err) {
    ElMessage.error('获取模版列表失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (row) => {
  Object.assign(editForm, row)
  editDialogVisible.value = true
}

const handleSave = async () => {
  try {
    const submitData = { ...editForm }
    await updateTemplateApi.update(submitData)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    getList()
  } catch (err) {
    ElMessage.error('更新失败')
  }
}

const goBack = () => {
  router.push('/admin/museum-config')
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.message-template-container {
  padding: 20px;
}
.card-header {
  font-weight: bold;
}
.tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
