<template>
  <div class="notice-manage-container">
    <el-page-header @back="goBack" content="场馆公告列表" style="margin-bottom: 20px;" v-if="!isEdit" />
    <el-page-header @back="cancelEdit" content="编辑公告" style="margin-bottom: 20px;" v-else />

    <!-- 公告列表/编辑切换 -->
    <div v-if="!isEdit" class="notice-list">
      <el-card>
        <div slot="header" class="card-header">
          <span>场馆公告列表</span>
          <el-button type="primary" size="small" @click="handleAdd">新建</el-button>
        </div>

        <!-- 新增：搜索功能 -->
        <div class="search-box" style="margin-bottom: 20px;">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入公告标题/内容关键词搜索"
            style="width: 300px;"
            clearable
            @keyup.enter="getNoticeList"
          >
            <template #append>
              <el-button @click="getNoticeList">搜索</el-button>
            </template>
          </el-input>
        </div>

        <el-table 
          :data="noticeList" 
          border 
          stripe 
          :loading="loading"
          @row-click="handleRowClick" 
        >
          <el-table-column prop="newsTitle" label="公告标题" min-width="200" />
          <el-table-column prop="newsDesc" label="公告内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="newsAddTime" label="发布时间" width="180" :formatter="formatTime" />
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button
                type="primary"
                size="small"
                @click="handleEdit(scope.row)"
              >
                编辑
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="handleDelete(scope.row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-button style="margin-top: 20px;" @click="goBack">返回</el-button>
      </el-card>
    </div>

    <!-- 公告编辑/新增表单 -->
    <div v-else class="notice-form">
      <el-card>
        <el-form :model="noticeForm" ref="noticeRef" label-width="100px">
          <el-form-item label="通知标题" prop="newsTitle">
            <el-input v-model="noticeForm.newsTitle" placeholder="请输入公告标题" />
          </el-form-item>
          <el-form-item label="通知内容" prop="newsDesc">
            <el-input
              v-model="noticeForm.newsDesc"
              type="textarea"
              :rows="8"
              placeholder="请输入公告内容"
            />
          </el-form-item>
          <!-- 移除：发布时间表单项 -->

          <!-- 操作按钮 -->
          <el-form-item>
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" @click="handleSave">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 新增：公告详情弹窗 -->
    <el-dialog 
      title="公告详情" 
      v-model="detailDialogVisible" 
      width="500px"
      center
    >
      <div class="notice-detail">
        <p class="detail-title"><span>标题：</span>{{ currentNotice.newsTitle }}</p>
        <p class="detail-content"><span>内容：</span>{{ currentNotice.newsDesc }}</p>
        <p class="detail-status"><span>状态：</span>{{ currentNotice.newsStatus === 1 ? '已发布' : '未发布' }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  getNoticeListApi,
  addNoticeApi,
  editNoticeApi,
  deleteNoticeApi,
  getNoticeDetailApi // 替换原publishNoticeApi
} from '@/api/notice'

const router = useRouter()
const isEdit = ref(false)
const loading = ref(false)
const noticeRef = ref(null)

// 新增：搜索关键词
const searchKeyword = ref('')

// 详情弹窗相关状态
const detailDialogVisible = ref(false)
const currentNotice = reactive({
  newsTitle: '',
  newsDesc: '',
  newsStatus: ''
})

// 公告列表
const noticeList = ref([])

// 公告表单
const noticeForm = reactive({
  id: '',
  newsTitle: '',
  newsDesc: ''
})

// 时间格式化
const formatTime = (row, column, cellValue) => {
  if (!cellValue) return ''
  const date = new Date(cellValue)
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 获取公告列表（适配后端：传递keyword/page/limit参数）
const getNoticeList = async () => {
  try {
    loading.value = true
    // 调用列表接口，传递搜索关键词、分页参数
    const res = await getNoticeListApi.getList({
      keyword: searchKeyword.value,
      page: 1,
      limit: 10
    })
    // 适配后端Page返回结构：records是列表数据
    noticeList.value = res.data.records || res.data
  } catch (err) {
    console.error('获取公告列表失败：', err)
    ElMessage.error('获取公告列表失败')
  } finally {
    loading.value = false
  }
}

// 行点击展示详情：调用详情接口
const handleRowClick = async (row) => {
  try {
    // 调用详情接口，传递行ID
    const res = await getNoticeDetailApi.getDetail(row.id)
    if (res.code === 200) {
      // 将接口返回的详情数据赋值给currentNotice
      Object.assign(currentNotice, res.data)
      detailDialogVisible.value = true
    } else {
      ElMessage.error('获取公告详情失败')
    }
  } catch (err) {
    console.error('获取公告详情失败：', err)
    ElMessage.error('获取公告详情失败')
  }
}

// 新增公告
const handleAdd = () => {
  isEdit.value = true
  Object.assign(noticeForm, {
    id: '',
    newsTitle: '',
    newsDesc: ''
  })
}

// 编辑公告
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(noticeForm, {
    id: row.id,
    newsTitle: row.newsTitle,
    newsDesc: row.newsDesc
  })
}

// 删除公告（调用后端del接口，传递id参数）
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该公告？', '提示', {
      type: 'warning'
    })
    // 调用后端删除接口，传递id参数
    await deleteNoticeApi.delete(id)
    ElMessage.success('删除成功')
    getNoticeList() // 重新获取列表
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除公告失败：', err)
      ElMessage.error('删除公告失败')
    }
  }
}

// 保存公告（适配后端字段：newsTitle/newsDesc）
const handleSave = async () => {
  try {
    // 表单校验
    await noticeRef.value.validate()
    
    // 适配后端字段：newsTitle=标题，newsDesc=内容，newsStatus=1(正常)
    const submitData = {
      newsTitle: noticeForm.newsTitle,
      newsDesc: noticeForm.newsDesc,
      newsStatus: 1
    }

    if (noticeForm.id) {
      // 编辑：补充id字段
      submitData.id = noticeForm.id
      await editNoticeApi.edit(submitData)
      ElMessage.success('编辑成功')
    } else {
      // 新增
      await addNoticeApi.add(submitData)
      ElMessage.success('新增成功')
    }
    isEdit.value = false
    getNoticeList() // 重新获取列表
  } catch (err) {
    console.error('保存公告失败：', err)
    ElMessage.error('保存公告失败')
  }
}

// 取消编辑
const cancelEdit = () => {
  isEdit.value = false
}

// 返回上一页
const goBack = () => {
  router.push('/admin/museum-config')
}

// 初始化加载列表
onMounted(() => {
  getNoticeList()
})
</script>

<style scoped>
.notice-manage-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.notice-form {
  padding: 0 20px;
}
.notice-detail {
  padding: 10px 0;
}
.detail-title, .detail-content, .detail-status {
  margin: 15px 0;
  line-height: 1.6;
}
.detail-title span, .detail-content span, .detail-status span {
  font-weight: 600;
  margin-right: 10px;
}
.detail-content {
  white-space: pre-wrap;
  word-break: break-all;
}
.search-box {
  display: flex;
  align-items: center;
}
</style>