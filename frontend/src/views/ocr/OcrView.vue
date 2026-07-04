<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AppUpload from '../../components/common/AppUpload.vue';
import AppTable from '../../components/common/AppTable.vue';
import JsonViewer from '../../components/common/JsonViewer.vue';
import TaskProgress from '../../components/common/TaskProgress.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { fetchOcrRecord, updateOcrFields } from '../../api/ocr';
import { fetchTaskStages } from '../../api/task';
import type { OcrRecord, TaskStageLog } from '../../api/types';

const loading = ref(false);
const error = ref('');
const record = ref<OcrRecord | null>(null);
const logs = ref<TaskStageLog[]>([]);

async function loadData() {
  loading.value = true;
  error.value = '';
  try {
    record.value = await fetchOcrRecord(11001);
    logs.value = await fetchTaskStages(record.value.taskId);
  } catch (err) { error.value = err instanceof Error ? err.message : 'OCR结果加载失败'; }
  finally { loading.value = false; }
}

async function saveFields() {
  if (!record.value) return;
  loading.value = true;
  try { await updateOcrFields(record.value.recordId, record.value.fields); }
  catch (err) { error.value = err instanceof Error ? err.message : '保存修订失败'; }
  finally { loading.value = false; }
}

onMounted(loadData);
</script>

<template>
  <div class="page"><el-alert v-if="error" :title="error" type="error" show-icon /><div class="page-header"><div><h2 class="page-title">OCR识别</h2><p class="page-desc">图片/文档上传、识别进度、结构化字段、人工修订和JSON下载。</p></div><el-button type="primary" :loading="loading" @click="saveFields">保存修订</el-button></div><EmptyState v-if="!loading && !error && !record" description="暂无OCR记录" /><template v-else-if="record"><div class="two-col"><el-card class="work-card"><h3 class="panel-title">文件上传与预览</h3><AppUpload accept=".jpg,.jpeg,.png,.pdf" tip="支持身份证、车牌、发票、合同和自定义文档" /><div class="preview">文件预览区</div></el-card><el-card class="work-card" v-loading="loading"><h3 class="panel-title">识别字段</h3><TaskProgress :percentage="record.progress" :status="record.status" :logs="logs" /><AppTable :data="record.fields" :columns="[{prop:'fieldName',label:'字段'},{prop:'fieldValue',label:'识别值'},{prop:'confidence',label:'置信度'},{prop:'location',label:'位置'}]"><template #empty><EmptyState description="暂无识别字段" /></template><el-table-column label="修订"><template #default="{ row }"><el-input v-model="row.fieldValue" /></template></el-table-column></AppTable></el-card></div><JsonViewer :value="record" title="OCR JSON结果" /></template></div>
</template>
<style scoped>.preview{height:220px;margin-top:14px;border:1px dashed var(--sw-border);border-radius:12px;display:grid;place-items:center;color:var(--sw-muted);background:#f8fafc}</style>
