<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import StatusTag from '../../components/common/StatusTag.vue';
import TaskProgress from '../../components/common/TaskProgress.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { useProjectStore } from '../../stores/project';
import { fetchReports } from '../../api/report';
import type { ReportItem } from '../../api/types';

const projectStore = useProjectStore();
const loading = ref(false);
const error = ref('');
const reports = ref<ReportItem[]>([]);
const tasks = computed(() => reports.value.map((item) => ({ name: item.reportName, status: item.status, progress: item.progress })));
const metrics = computed(() => [
  { label: '知识库数量', value: 6 },
  { label: '待处理任务', value: tasks.value.filter((item) => item.status === 'PROCESSING').length },
  { label: '最近报告', value: reports.value.length },
  { label: 'OCR处理中', value: 1 }
]);

async function loadData() {
  loading.value = true;
  error.value = '';
  try {
    if (!projectStore.currentProject) await projectStore.fetchProjects();
    const page = await fetchReports({ projectId: projectStore.currentProject?.projectId, pageNo: 1, pageSize: 5 });
    reports.value = page.records;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '首页数据加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <div class="page" v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon />
    <div class="page-header"><div><h2 class="page-title">首页工作台</h2><p class="page-desc">{{ projectStore.currentProject?.name || '暂无项目' }} 的任务状态、资料处理和智能能力入口。</p></div></div>
    <div v-if="!loading && !error && !reports.length"><EmptyState description="暂无工作台数据" action-text="刷新" @action="loadData" /></div>
    <template v-else>
      <div class="card-grid"><el-card v-for="item in metrics" :key="item.label" class="work-card"><div class="muted">{{ item.label }}</div><div class="metric">{{ item.value }}</div></el-card></div>
      <div class="two-col"><el-card class="work-card"><h3 class="panel-title">快捷入口</h3><el-space wrap><el-button type="primary" @click="$router.push('/qa')">知识问答</el-button><el-button @click="$router.push('/review')">发起审查</el-button><el-button @click="$router.push('/report')">报告管理</el-button><el-button @click="$router.push('/ocr')">OCR识别</el-button></el-space></el-card><el-card class="work-card"><h3 class="panel-title">最近任务</h3><EmptyState v-if="!tasks.length" description="暂无任务" /><div v-for="task in tasks" :key="task.name" style="margin-bottom:16px"><div style="display:flex;justify-content:space-between;margin-bottom:8px"><span>{{ task.name }}</span><StatusTag :status="task.status" /></div><TaskProgress :percentage="task.progress" :status="task.status" /></div></el-card></div>
    </template>
  </div>
</template>
