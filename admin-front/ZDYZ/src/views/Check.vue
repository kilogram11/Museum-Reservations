<template>
  <div class="check-container">
    <!-- 搜索栏 -->
    <el-card style="margin-bottom: 20px;">
      <el-form :model="searchForm" inline @submit.prevent="handleSearch">
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="输入姓名" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="searchForm.idCard" placeholder="输入身份证号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 核销列表 -->
    <el-card>
      <el-table :data="checkList" border stripe :loading="loading">
        <el-table-column label="姓名" width="120">
          <template #default="scope">
            {{ parseForm(scope.row.joinForms, 'name') }}
          </template>
        </el-table-column>
        <el-table-column label="身份证号" width="200">
          <template #default="scope">
            {{ parseForm(scope.row.joinForms, 'card') }}
          </template>
        </el-table-column>
        <el-table-column prop="joinMeetDay" label="预约日期" width="120" />
        <el-table-column label="时间段" width="150">
          <template #default="scope">
            {{ scope.row.joinMeetTimeStart }} - {{ scope.row.joinMeetTimeEnd }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row)">
              {{ getStatusText(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button
              v-if="scope.row.joinIsCheckin === 0 && scope.row.joinStatus === 1"
              type="primary"
              size="small"
              @click="handleCheck(scope.row.id || scope.row.joinId)"
            >
              核销
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        style="margin-top: 20px; text-align: right;"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="page"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="limit"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
      >
      </el-pagination>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCheckListApi, checkBookingApi } from '@/api/check'

// 搜索表单
const searchForm = reactive({
  name: '',
  idCard: ''
})

// 列表数据
const checkList = ref([])
const loading = ref(false)
const page = ref(1)
const limit = ref(10)
const total = ref(0)

// 获取核销列表
const getCheckList = async () => {
  try {
    loading.value = true
    // 分别传递name和idCard参数
    const params = {
      name: searchForm.name,
      idCard: searchForm.idCard,
      page: page.value,
      limit: limit.value
    }
    const res = await getCheckListApi.getList(params)
    checkList.value = res.data.records
    total.value = res.data.total
  } catch (err) {
    console.error(err)
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  page.value = 1
  getCheckList()
}

// 重置
const handleReset = () => {
  searchForm.name = ''
  searchForm.idCard = ''
  page.value = 1
  getCheckList()
}

// 分页大小改变
const handleSizeChange = (val) => {
  limit.value = val
  getCheckList()
}

// 页码改变
const handleCurrentChange = (val) => {
  page.value = val
  getCheckList()
}

// 导出
const handleExport = async () => {
  try {
    ElMessage.info('正在下载，请稍候...')
    // 导出所有，不需要传分页参数
    const res = await checkBookingApi.exportData()
    // 下载 blob
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    // 尝试获取文件名
    const disposition = res.headers['content-disposition']
    let fileName = '预约记录.xlsx'
    if (disposition) {
      const match = disposition.match(/filename=(.+)/)
      if (match && match[1]) {
        fileName = decodeURIComponent(match[1])
      }
    }
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (e) {
    console.error(e)
    ElMessage.error('导出失败')
  }
}

// 辅助函数：解析JSON
const parseForm = (jsonStr, field) => {
  try {
    if (!jsonStr) return '-'
    const obj = JSON.parse(jsonStr)
    return obj[field] || '-'
  } catch (e) {
    return '-'
  }
}

// 辅助函数：获取状态文本
const getStatusText = (row) => {
  if (row.joinStatus === 2) return '已取消'
  if (row.joinIsCheckin === 1) return '已入馆'
  if (row.joinIsCheckin === 3) return '爽约'
  return '未入馆'
}

// 辅助函数：获取状态标签类型
const getStatusType = (row) => {
  if (row.joinStatus === 2) return 'info'
  if (row.joinIsCheckin === 1) return 'success'
  if (row.joinIsCheckin === 3) return 'danger'
  return 'warning'
}

// 核销操作
const handleCheck = async (id) => {
  try {
    await checkBookingApi.check(id)
    ElMessage.success('核销成功')
    getCheckList()
  } catch (err) {
    console.error(err)
    ElMessage.error(err.response?.data?.msg || '核销失败')
  }
}

onMounted(() => {
  getCheckList()
})
</script>

<style scoped>
.check-container {
  padding: 20px;
}
</style>