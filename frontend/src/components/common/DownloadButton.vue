<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { downloadFile } from '../../utils/request';

const props = defineProps<{ filename?: string; content?: string; url?: string; label?: string; params?: Record<string, unknown>; method?: string }>();

async function download() {
  try {
    if (props.url) {
      await downloadFile(props.url, { filename: props.filename, params: props.params, method: props.method || 'GET' });
      return;
    }
    const blob = new Blob([props.content || ''], { type: 'text/plain;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = props.filename || 'download.txt';
    a.click();
    URL.revokeObjectURL(a.href);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载失败');
  }
}
</script>

<template><el-button type="primary" plain @click="download">{{ label || '下载' }}</el-button></template>
