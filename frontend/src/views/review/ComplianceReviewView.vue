<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AppUpload from '../../components/common/AppUpload.vue';
import AppTable from '../../components/common/AppTable.vue';
import JsonViewer from '../../components/common/JsonViewer.vue';
import TaskProgress from '../../components/common/TaskProgress.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { fetchReviewRecord, fetchReviewTemplates } from '../../api/review';
import { fetchTaskStages } from '../../api/task';
import { useProjectStore } from '../../stores/project';
import type { ReviewRecord, ReviewTemplate, TaskStageLog } from '../../api/types';

const projectStore = useProjectStore();
const loading = ref(false);
const error = ref('');
const templates = ref<ReviewTemplate[]>([]);
const currentRecord = ref<ReviewRecord | null>(null);
const logs = ref<TaskStageLog[]>([]);

async function loadData() {
  loading.value = true;
  error.value = '';
  try {
    if (!projectStore.currentProject) await projectStore.fetchProjects();
    templates.value = await fetchReviewTemplates(projectStore.currentProject?.projectId);
    currentRecord.value = await fetchReviewRecord(8001);
    logs.value = await fetchTaskStages(currentRecord.value.taskId);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '审查数据加载失败';
  } finally { loading.value = false; }
}

onMounted(loadData);
</script>

<template>
  <div class="page" v-loading="loading"><el-alert v-if="error" :title="error" type="error" show-icon /><div class="page-header"><div><h2 class="page-title">合规审查</h2><p class="page-desc">上传方案或合同，按模板生成问题定位、修改建议和JSON结果。</p></div></div><el-card class="work-card"><el-form inline><el-form-item label="审查模板"><el-select :model-value="templates[0]?.templateId" style="width:220px" placeholder="请选择模板"><el-option v-for="item in templates" :key="item.templateId" :label="item.templateName" :value="item.templateId" /></el-select></el-form-item><el-form-item><el-button type="primary" :loading="loading">发起审查</el-button></el-form-item></el-form><AppUpload accept=".doc,.docx,.pdf" /></el-card><EmptyState v-if="!loading && !error && !currentRecord" description="暂无审查记录" /><template v-else-if="currentRecord"><el-card class="work-card"><h3 class="panel-title">审查进度</h3><TaskProgress :percentage="currentRecord.progress" :status="currentRecord.status" :logs="logs" /></el-card><div class="two-col"><el-card class="work-card"><h3 class="panel-title">问题列表</h3><AppTable :data="currentRecord.issues" :columns="[{prop:'severity',label:'严重程度'},{prop:'location',label:'问题定位'},{prop:'description',label:'问题描述'},{prop:'suggestion',label:'修改建议'}]"><template #empty><EmptyState description="暂无审查问题" /></template><el-table-column label="状态"><template #default><StatusTag :status="currentRecord?.status" /></template></el-table-column></AppTable></el-card><JsonViewer :value="currentRecord" /></div></template></div>
</template>
