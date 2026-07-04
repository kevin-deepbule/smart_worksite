<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import DownloadButton from '../../components/common/DownloadButton.vue';
import JsonViewer from '../../components/common/JsonViewer.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import TaskProgress from '../../components/common/TaskProgress.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { fetchReportDetail } from '../../api/report';
import { fetchTaskStages } from '../../api/task';
import type { ReportItem, TaskStageLog } from '../../api/types';

const route = useRoute();
const loading = ref(false);
const error = ref('');
const report = ref<ReportItem | null>(null);
const logs = ref<TaskStageLog[]>([]);

async function loadData() {
  loading.value = true;
  error.value = '';
  try {
    report.value = await fetchReportDetail(route.params.id as string);
    logs.value = await fetchTaskStages(report.value.taskId);
  } catch (err) { error.value = err instanceof Error ? err.message : '报告详情加载失败'; }
  finally { loading.value = false; }
}
onMounted(loadData);
</script>

<template>
  <div class="page" v-loading="loading"><el-alert v-if="error" :title="error" type="error" show-icon /><EmptyState v-if="!loading && !error && !report" description="报告不存在" /><template v-else-if="report"><div class="page-header"><div><h2 class="page-title">报告详情</h2><p class="page-desc">版本、状态、预览、下载和重新生成。</p></div><el-space><DownloadButton :url="`/reports/${report.reportId}/download`" :params="{ format: 'WORD' }" :filename="`${report.reportName}.docx`" label="下载报告" /><el-button>重新生成</el-button></el-space></div><el-card class="work-card"><el-descriptions :column="3" border><el-descriptions-item label="报告名称">{{ report.reportName }}</el-descriptions-item><el-descriptions-item label="版本">{{ report.version }}</el-descriptions-item><el-descriptions-item label="状态"><StatusTag :status="report.status" /></el-descriptions-item></el-descriptions></el-card><div class="two-col"><el-card class="work-card"><h3 class="panel-title">报告预览</h3><p>本报告基于项目知识库、检查记录和AI生成内容形成。</p><p class="muted">这里后续接入Word/PDF在线预览。</p></el-card><el-card class="work-card"><h3 class="panel-title">生成进度</h3><TaskProgress :percentage="report.progress" :status="report.status" :logs="logs" /></el-card></div><JsonViewer :value="report" title="报告元数据" /></template></div>
</template>
