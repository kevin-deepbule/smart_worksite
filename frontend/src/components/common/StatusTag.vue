<script setup lang="ts">
const props = defineProps<{ status?: string; text?: string }>();

const typeMap: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
  ACTIVE: 'success',
  ENABLED: 'success',
  SUCCESS: 'success',
  COMPLETED: 'success',
  DONE: 'success',
  GENERATING: 'warning',
  PROCESSING: 'warning',
  RUNNING: 'warning',
  PENDING: 'warning',
  FAILED: 'danger',
  ERROR: 'danger',
  DISABLED: 'info',
  ARCHIVED: 'info',
  CANCELLED: 'info'
};

const textMap: Record<string, string> = {
  ACTIVE: '正常',
  ENABLED: '启用',
  SUCCESS: '成功',
  COMPLETED: '已完成',
  DONE: '已完成',
  GENERATING: '生成中',
  PROCESSING: '处理中',
  RUNNING: '运行中',
  PENDING: '待处理',
  FAILED: '失败',
  ERROR: '异常',
  DISABLED: '停用',
  ARCHIVED: '已归档',
  CANCELLED: '已取消'
};
</script>

<template>
  <el-tag :type="typeMap[props.status || ''] || 'info'">
    {{ text || textMap[props.status || ''] || status || '未知' }}
  </el-tag>
</template>
