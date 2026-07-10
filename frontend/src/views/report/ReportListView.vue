<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import AppSearchForm from '../../components/common/AppSearchForm.vue';
import AppTable from '../../components/common/AppTable.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { createReport, downloadReport, fetchReportTemplates, fetchReports, type ReportTemplate } from '../../api/report';
import { useProjectStore } from '../../stores/project';
import type { ID, ReportItem } from '../../api/types';

const router = useRouter();
const projectStore = useProjectStore();
const loading = ref(false);
const creating = ref(false);
const dialogVisible = ref(false);
const error = ref('');
const templateWarning = ref('');
const search = reactive({ keyword: '', status: '' });
const reports = ref<ReportItem[]>([]);
const templates = ref<ReportTemplate[]>([]);
const downloadingId = ref<ID>('');
const form = reactive<{ reportName: string; reportType: string; templateId: ID | '' }>({
  reportName: '',
  reportType: 'SAFETY_MONTHLY',
  templateId: ''
});

const templateEmpty = computed(() => !templates.value.length && !templateWarning.value);
const canCreate = computed(() => Boolean(form.reportName.trim() && form.reportType && form.templateId));

async function loadData() {
  loading.value = true;
  error.value = '';
  try {
    if (!projectStore.currentProject) await projectStore.fetchProjects();
    const page = await fetchReports({
      projectId: projectStore.currentProject?.projectId,
      keyword: search.keyword,
      status: search.status,
      pageNo: 1,
      pageSize: 20
    });
    reports.value = page.records;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '报告列表加载失败';
  } finally {
    loading.value = false;
  }
}

async function openCreateDialog() {
  dialogVisible.value = true;
  templateWarning.value = '';
  templates.value = [];
  form.templateId = '';
  try {
    templates.value = await fetchReportTemplates(projectStore.currentProject?.projectId);
    if (templates.value[0]) form.templateId = templates.value[0].templateId;
  } catch (err) {
    templateWarning.value = err instanceof Error ? err.message : '报告模板接口暂不可用，请稍后重试';
  }
}

function reset() {
  search.keyword = '';
  search.status = '';
  loadData();
}

async function submitCreate() {
  if (!form.reportName.trim()) return ElMessage.warning('请填写报告名称');
  if (!form.reportType) return ElMessage.warning('请选择报告类型');
  if (!form.templateId) return ElMessage.warning('请选择报告模板');
  creating.value = true;
  error.value = '';
  try {
    const result = await createReport({
      projectId: projectStore.currentProject?.projectId || 0,
      reportName: form.reportName,
      reportType: form.reportType,
      templateId: form.templateId
    });
    if (result.status === 'FAILED') {
      ElMessage.warning('报告任务已创建，但生成失败，请进入详情查看失败原因');
    } else {
      ElMessage.success('报告创建成功');
    }
    dialogVisible.value = false;
    form.reportName = '';
    await loadData();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '报告创建失败，请确认后端 /api/reports 是否可用';
  } finally {
    creating.value = false;
  }
}

async function handleDownload(row: ReportItem) {
  downloadingId.value = row.reportId;
  error.value = '';
  try {
    await downloadReport(row.reportId, 'WORD', `${row.reportName}.docx`);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '报告下载失败';
  } finally {
    downloadingId.value = '';
  }
}

function goTemplates() {
  dialogVisible.value = false;
  router.push('/templates');
}

onMounted(loadData);
</script>

<template>
  <div class="page">
    <el-alert v-if="error" :title="error" type="error" show-icon />
    <div class="page-header">
      <div>
        <h2 class="page-title">报告管理</h2>
        <p class="page-desc">报告列表、版本、预览、下载、重新生成和归档。</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">新建报告</el-button>
    </div>

    <AppSearchForm
      v-model="search"
      :fields="[
        { prop: 'keyword', label: '关键词' },
        { prop: 'status', label: '状态', type: 'select', options: [
          { label: '已完成', value: 'COMPLETED' },
          { label: '生成中', value: 'GENERATING' },
          { label: '失败', value: 'FAILED' }
        ] }
      ]"
      @search="loadData"
      @reset="reset"
    />

    <el-card class="work-card">
      <AppTable
        :loading="loading"
        :data="reports"
        :error="error"
        :total="reports.length"
        :columns="[
          { prop: 'reportName', label: '报告名称' },
          { prop: 'version', label: '版本' },
          { prop: 'status', label: '状态', slot: 'status' },
          { prop: 'createdBy', label: '创建人' },
          { prop: 'updatedAt', label: '更新时间' }
        ]"
      >
        <template #empty><EmptyState description="暂无报告，可先上传报告模板后创建报告。" /></template>
        <template #status="{ row }"><StatusTag :status="row.status" /></template>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/report/${row.reportId}`)">详情</el-button>
            <el-button link :loading="String(downloadingId) === String(row.reportId)" @click="handleDownload(row)">下载</el-button>
            <el-button link disabled title="后端接口待提供">分享</el-button>
            <el-button link disabled title="后端接口待提供">归档</el-button>
          </template>
        </el-table-column>
      </AppTable>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新建报告" width="560px">
      <el-alert v-if="templateWarning" :title="templateWarning" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
      <el-alert
        v-else-if="templateEmpty"
        title="当前项目暂无报告模板，请先到模板中心上传报告模板，或等待后端提供默认模板策略。"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 12px"
      >
        <template #default><el-button type="primary" link @click="goTemplates">去模板中心</el-button></template>
      </el-alert>

      <el-form label-width="96px">
        <el-form-item label="报告名称"><el-input v-model="form.reportName" placeholder="请输入报告名称" /></el-form-item>
        <el-form-item label="报告类型">
          <el-select v-model="form.reportType" style="width: 100%">
            <el-option label="安全月报" value="SAFETY_MONTHLY" />
            <el-option label="质量周报" value="QUALITY_WEEKLY" />
            <el-option label="综合报告" value="GENERAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告模板">
          <el-select v-model="form.templateId" style="width: 100%" placeholder="请选择报告模板" clearable>
            <el-option v-for="item in templates" :key="item.templateId" :label="item.templateName" :value="item.templateId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" :disabled="!canCreate" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>
