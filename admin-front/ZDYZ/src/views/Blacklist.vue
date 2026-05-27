<template>
  <div class="blacklist-container">
    <!-- 搜索栏：拆分姓名、身份证号、手机号输入框 -->
    <el-card style="margin-bottom: 20px;">
      <el-form :model="searchForm" inline @submit.prevent="handleSearch">
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="输入姓名" style="width: 160px" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="searchForm.idCard" placeholder="输入身份证号" style="width: 180px" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="输入手机号" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleExport">导出</el-button>
          <el-button type="danger" @click="handleOpenManualBlack">手动拉黑</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 黑名单列表：保持原有逻辑不变 -->
    <el-card>
      <el-table 
        :data="blacklist" 
        border 
        stripe 
        :loading="loading"
      >
        <el-table-column prop="identityName" label="姓名" width="120" />
        <el-table-column prop="identityCard" label="身份证号" width="200" />
        <el-table-column prop="identityMobile" label="手机号" width="150" />
        <el-table-column label="拉黑原因" min-width="200">
          <template #default="scope">
            {{ scope.row.identityStatus === 0 ? (getBlackReason(scope.row) || '爽约次数过多') : '' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button
              :type="scope.row.identityStatus === 0 ? 'primary' : 'danger'"
              size="small"
              @click="scope.row.identityStatus === 0 ? handleCancelBlack(scope.row) : handleShowBlackDialog(scope.row)"
            >
              {{ scope.row.identityStatus === 0 ? '取消拉黑' : '拉黑' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        style="margin-top: 20px; text-align: right;"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pageNum"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
      >
      </el-pagination>
    </el-card>

    <!-- 手动拉黑弹窗：搜索并选择正常用户 -->
    <el-dialog
      v-model="manualDialogVisible"
      title="手动添加黑名单"
      width="700px"
    >
      <div class="manual-search">
         <el-input v-model="manualKeyword" placeholder="请输入姓名/手机号/身份证搜索" style="width: 300px; margin-bottom: 20px;">
           <template #append>
             <el-button @click="searchNormalUsers">搜索</el-button>
           </template>
         </el-input>
      </div>
      
      <el-table :data="normalUserList" border stripe height="300px" v-loading="manualLoading">
         <el-table-column prop="identityName" label="姓名" width="100" />
         <el-table-column prop="identityCard" label="身份证号" width="180" />
         <el-table-column prop="identityMobile" label="手机号" width="140" />
         <el-table-column label="操作" width="100">
           <template #default="scope">
             <el-button type="danger" size="small" @click="handleSelectUser(scope.row)">拉黑</el-button>
           </template>
         </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 填写原因弹窗 -->
    <el-dialog
      v-model="blackDialogVisible"
      title="填写拉黑原因"
      width="500px"
    >
      <el-form :model="blackForm" ref="blackRef" label-width="100px">
        <el-form-item label="拉黑对象">
           <span>{{ currentBlackName }} ({{ blackForm.idCard }})</span>
           <!-- 显示一下选中的人，防止误操作。注意 blackForm.idCard 存的是 ID -->
        </el-form-item>
        <el-form-item label="拉黑原因" prop="reason" :rules="{ required: true, message: '请输入拉黑原因', trigger: 'blur' }">
          <el-input
            v-model="blackForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拉黑原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="blackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmBlack">确定拉黑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBlacklistApi,
  cancelBlackApi,
  addBlackApi
} from '@/api/blacklist'

// 搜索表单：拆分为姓名、身份证号、手机号独立字段
const searchForm = reactive({
  name: '',     // 姓名
  idCard: '',   // 身份证号
  phone: ''     // 手机号
})

// 黑名单列表：保持原有逻辑不变
const blacklist = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 手动拉黑功能相关状态
const manualDialogVisible = ref(false)
const manualKeyword = ref('')
const manualLoading = ref(false)
const normalUserList = ref([])
const currentBlackName = ref('') // 用于在原因弹窗显示姓名

// 原因弹窗相关
const blackDialogVisible = ref(false)
const blackRef = ref(null)
const blackForm = reactive({
  idCard: '', // 实际存储 identityId (业务ID)
  reason: ''
})

// 辅助函数: 递归解析 JSON
const parseObj = (data) => {
  if (!data) return {}
  let res = data
  try {
    for (let i = 0; i < 3; i++) {
        if (typeof res === 'object') return res
        if (typeof res === 'string') {
            if (res.trim().startsWith('{') || res.trim().startsWith('[')) {
                res = JSON.parse(res)
            } else {
                return {}
            }
        }
    }
  } catch (e) {
    return {}
  }
  return typeof res === 'object' ? res : {}
}

// 获取拉黑原因
const getBlackReason = (row) => {
  const obj = parseObj(row.identityObj)
  return obj.blackReason
}

// 获取黑名单列表
const getBlacklist = async () => {
  try {
    loading.value = true
    // 构造搜索参数：合并字段为 keyword 传给后端
    // 后端使用 unified keyword search (OR logic)
    const keyword = searchForm.name.trim() || searchForm.idCard.trim() || searchForm.phone.trim() || ''
    
    const params = {
      keyword: keyword,
      page: pageNum.value,
      limit: pageSize.value,
      status: 0 // 明确查询黑名单用户
    }
    const res = await getBlacklistApi.list(params)
    blacklist.value = res.data.records
    total.value = res.data.total
  } catch (err) {
    console.error('获取黑名单列表失败：', err)
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

// 打开手动拉黑弹窗
const handleOpenManualBlack = () => {
  manualDialogVisible.value = true
  manualKeyword.value = ''
  normalUserList.value = []
}

// 搜索正常用户 (用于手动拉黑)
const searchNormalUsers = async () => {
  if (!manualKeyword.value) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  try {
    manualLoading.value = true
    const res = await getBlacklistApi.list({
       keyword: manualKeyword.value,
       page: 1,
       limit: 20, // 只展示前20条匹配结果
       status: 1 // 查询正常用户
    })
    normalUserList.value = res.data.records
  } catch (err) {
    ElMessage.error('搜索失败')
  } finally {
    manualLoading.value = false
  }
}

// 选中用户去拉黑
const handleSelectUser = (row) => {
   // 打开原因弹窗
   blackForm.idCard = row.identityId // 绑定业务ID
   blackForm.reason = ''
   currentBlackName.value = row.identityName
   blackDialogVisible.value = true
   // 暂时不关 manualDialog，等确认后再关，或者现在关也行。为了体验更好，可以先不关。
}

// 搜索：保持原有逻辑
const handleSearch = () => {
  pageNum.value = 1
  getBlacklist()
}

// 导出黑名单
const handleExport = async () => {
  try {
    ElMessage.info('正在下载，请稍候...')
    const res = await getBlacklistApi.exportData()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    let fileName = '黑名单记录.xlsx'
    const disposition = res.headers['content-disposition']
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

// 重置搜索：清空所有独立搜索字段
const handleReset = () => {
  searchForm.name = ''
  searchForm.idCard = ''
  searchForm.phone = ''
  pageNum.value = 1
  getBlacklist()
}

// 分页相关：保持原有逻辑
const handleSizeChange = (val) => {
  pageSize.value = val
  getBlacklist()
}
const handleCurrentChange = (val) => {
  pageNum.value = val
  getBlacklist()
}

// 取消拉黑
const handleCancelBlack = async (row) => {
  try {
    await ElMessageBox.confirm('确定取消该用户的拉黑？', '提示', {
      type: 'warning'
    })
    // 必须传递 identityId (业务ID) 而非身份证号
    await cancelBlackApi.remove({ identityId: row.identityId })
    
    const index = blacklist.value.findIndex(item => item.identityId === row.identityId)
    if (index > -1) {
      blacklist.value[index].identityStatus = 1 // 恢复正常状态
    }
    ElMessage.success('取消拉黑成功')
    getBlacklist() // 刷新列表以确保同步
  } catch (err) {
    console.error('取消拉黑失败：', err)
    ElMessage.error('取消拉黑失败')
  }
}

// 显示拉黑弹窗 (从黑名单列表重新拉黑? 逻辑上Status=0才会在列表，所以这一条其实用不上，除非列表有Bug显示了正常用户)
// 修正：如果列表里都是黑名单用户，这个按钮基本不会出现。
// 但保留此函数逻辑以防万一
const handleShowBlackDialog = (row) => {
  blackForm.idCard = row.identityId
  blackForm.reason = ''
  currentBlackName.value = row.identityName
  blackDialogVisible.value = true
}

// 确认手动拉黑
const handleConfirmBlack = async () => {
  try {
    await blackRef.value.validate()
    const endTime = Date.now() + 30 * 86400000
    await addBlackApi.add({
      identityId: blackForm.idCard, // 传递的是 identityId
      reason: blackForm.reason,
      endTime: endTime
    })
    ElMessage.success('拉黑成功')
    blackDialogVisible.value = false
    manualDialogVisible.value = false // 关闭搜索弹窗
    getBlacklist() // 刷新黑名单列表
  } catch (err) {
    console.error('拉黑失败：', err)
    ElMessage.error('拉黑失败')
  }
}

// 初始化加载列表
onMounted(() => {
  getBlacklist()
})
</script>

<style scoped>
.blacklist-container {
  padding: 20px;
}
</style>