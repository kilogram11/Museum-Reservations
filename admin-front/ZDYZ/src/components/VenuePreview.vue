<template>
  <el-dialog
    title="场馆详情"
    :visible="visible"
    @close="$emit('close')"
    width="60%"
    :append-to-body="true"
  >
    <div class="preview-container">
      <h2 class="preview-title">{{ previewData.name || '未设置场馆名称' }}</h2>
      <div class="preview-info">
        <p><strong>场馆简介：</strong>{{ previewData.desc || '无' }}</p>
        <p><strong>场馆地址：</strong>{{ previewData.address || '无' }}</p>
        <p><strong>联系电话：</strong>{{ previewData.phone || '无' }}</p>
        <p><strong>可预约提前天数：</strong>{{ previewData.advanceDays || 0 }}天</p>
      </div>
      <div class="preview-time-slots">
        <h3>参观时段及可预约人数</h3>
        <el-table :data="previewData.timeSlots || []" border stripe style="width: 100%;">
          <el-table-column prop="time" label="时段" />
          <el-table-column prop="num" label="可预约人数" />
        </el-table>
        <div v-if="(previewData.timeSlots || []).length === 0" class="empty-time-slots">无参观时段配置</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  previewData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['close'])

const handleClose = () => {
  emit('close')
}
</script>

<style scoped>
.preview-container {
  padding: 10px;
}

.preview-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #1A1D1F;
}

.preview-info {
  margin-bottom: 20px;
}

.preview-info p {
  margin: 8px 0;
  color: #6F767E;
}

.preview-time-slots h3 {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #1A1D1F;
}

.empty-time-slots {
  text-align: center;
  color: #999;
  padding: 20px;
}
</style>