<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import JsonViewer from '../../components/common/JsonViewer.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import TaskProgress from '../../components/common/TaskProgress.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { downloadReport, fetchReportDetail, regenerateReport } from '../../api/report';
import { fetchTaskStages } from '../../api/task';
import type { ReportItem, TaskStageLog } from '../../api/types';

const route = useRoute();
const loading = ref(false);
const downloading = ref(false);
const regenerating = ref(false);
const error = ref('');
const notFound = ref(false);
const report = ref<ReportItem | null>(null);
const logs = ref<TaskStageLog[]>([]);

function isNotFoundError(err: unknown) {
  const message = err instanceof Error ? err.message : String(err || '');
  return message.includes('404') || message.includes('不存在') || message.includes('not found') || message.includes('Not Found');
}

async function loadData() {
  loading.value = true;
  error.value = '';
  notFound.value = false;
  try {
    report.value = await fetchReportDetail(route.params.id as string);
    logs.value = report.value.taskId ? await fetchTaskStages(report.value.taskId) : [];
  } catch (err) {
    report.value = null;
    notFound.value = isNotFoundError(err);
    error.value = notFound.value ? '' : (err instanceof Error ? err.message : '报告详情加载失败');
  } finally {
    loading.value = false;
  }
}

async function handleDownload() {
  if (!report.value) return;
  downloading.value = true;
  error.value = '';
  try {
    await downloadReport(report.value.reportId, 'WORD', `${report.value.reportName}.docx`);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '报告下载失败，请检查后端下载地址是否可用';
  } finally {
    downloading.value = false;
  }
}

async function handleRegenerate() {
  if (!report.value) return;
  regenerating.value = true;
  error.value = '';
  try {
    await regenerateReport(report.value.reportId);
    await loadData();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '重新生成失败';
  } finally {
    regenerating.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <div class="page" v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon />
    <EmptyState v-if="!loading && (notFound || !report)" description="报告不存在或已删除" />
    <template v-else-if="report">
      <el-alert
        v-if="report.status === 'FAILED'"
        :title="report.errorMessage || '报告尚未生成成功'"
        type="error"
        show-icon
        style="margin-bottom: 12px"
      />
      <div class="page-header">
        <div>
          <h2 class="page-title">报告详情</h2>
          <p class="page-desc">版本、状态、预览、下载和重新生成。</p>
        </div>
        <el-space>
          <el-button type="primary" plain :loading="downloading" @click="handleDownload">下载报告</el-button>
          <el-button :loading="regenerating" @click="handleRegenerate">重新生成</el-button>
          <el-button disabled title="后端接口待提供">版本列表</el-button>
        </el-space>
      </div>

      <el-card class="work-card">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="报告名称">{{ report.reportName }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ report.version }}</el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :status="report.status" /></el-descriptions-item>
          <el-descriptions-item v-if="report.errorMessage" label="失败原因" :span="3">{{ report.errorMessage }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <div class="two-col">
        <el-card class="work-card">
          <h3 class="panel-title">报告预览</h3>
          <p>报告生成成功后可下载 Word 文件查看。</p>
          <p class="muted">在线预览接口待后端提供。</p>
        </el-card>
        <el-card class="work-card">
          <h3 class="panel-title">生成进度</h3>
          <TaskProgress :percentage="report.progress" :status="report.status" :logs="logs" />
        </el-card>
      </div>

      <JsonViewer :value="report" title="报告元数据" />
    </template>
  </div>
</template>
