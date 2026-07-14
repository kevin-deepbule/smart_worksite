<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import AppTable from '../../components/common/AppTable.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { fetchAuditLogs, fetchExternalCallLogs } from '../../api/audit';
import { useProjectStore } from '../../stores/project';
import type { AuditLog, ExternalCallLog } from '../../api/types';

const projectStore = useProjectStore();
const loading = ref(false);
const rows = ref<AuditLog[]>([]);
const externalRows = ref<ExternalCallLog[]>([]);
const error = ref('');
const externalError = ref('');
const activeTab = ref('operation');
const pager = reactive({ pageNo: 1, pageSize: 10, total: 0, objectType: '', action: '' });
const externalPager = reactive({ pageNo: 1, pageSize: 10, total: 0, serviceName: '', callType: '', status: '' });
const projectId = computed(() => projectStore.currentProject?.projectId);

async function loadRows() {
  if (!projectId.value) {
    rows.value = [];
    pager.total = 0;
    error.value = '请先选择项目';
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const page = await fetchAuditLogs({ projectId: projectId.value, pageNo: pager.pageNo, pageSize: pager.pageSize, objectType: pager.objectType || undefined, action: pager.action || undefined });
    rows.value = page.records;
    pager.total = page.total;
  } catch (err) {
    rows.value = [];
    pager.total = 0;
    error.value = err instanceof Error ? err.message : '审计日志加载失败';
  } finally {
    loading.value = false;
  }
}

async function loadExternalRows() {
  if (!projectId.value) {
    externalRows.value = [];
    externalPager.total = 0;
    externalError.value = '请先选择项目';
    return;
  }
  loading.value = true;
  externalError.value = '';
  try {
    const page = await fetchExternalCallLogs({
      projectId: projectId.value,
      pageNo: externalPager.pageNo,
      pageSize: externalPager.pageSize,
      serviceName: externalPager.serviceName || undefined,
      callType: externalPager.callType || undefined,
      status: externalPager.status || undefined
    });
    externalRows.value = page.records;
    externalPager.total = page.total;
  } catch (err) {
    externalRows.value = [];
    externalPager.total = 0;
    externalError.value = err instanceof Error ? err.message : '外部调用日志加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  if (!projectStore.currentProject) await projectStore.fetchProjects();
  await Promise.all([loadRows(), loadExternalRows()]);
});
</script>

<template>
  <div class="page">
    <div class="page-header"><div><h2 class="page-title">审计日志</h2><p class="page-desc">用户操作、外部调用、关键业务动作留痕。</p></div></div>
    <el-card class="work-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="操作日志" name="operation">
          <div class="filters"><strong>日志列表</strong><div><el-input v-model="pager.objectType" clearable placeholder="对象类型" style="width:140px" /><el-input v-model="pager.action" clearable placeholder="动作" style="width:160px;margin-left:8px" /><el-button type="primary" style="margin-left:8px" @click="loadRows">查询</el-button></div></div>
          <AppTable :loading="loading" :error="error" :data="rows" :total="pager.total" :page-no="pager.pageNo" :page-size="pager.pageSize" :columns="[{ prop: 'createdAt', label: '时间', width: 190 }, { prop: 'operatorId', label: '操作人ID', width: 110 }, { prop: 'action', label: '动作' }, { prop: 'objectType', label: '对象类型' }, { prop: 'objectId', label: '对象ID', width: 100 }, { prop: 'requestId', label: '请求ID', width: 180 }, { prop: 'ipAddress', label: 'IP', width: 130 }]" @page-change="(p, s) => { pager.pageNo = p; pager.pageSize = s; loadRows(); }">
            <template #empty><EmptyState description="暂无审计日志" /></template>
          </AppTable>
        </el-tab-pane>
        <el-tab-pane label="外部调用日志" name="external">
          <div class="filters"><strong>AI / OCR / 外部服务调用</strong><div><el-input v-model="externalPager.serviceName" clearable placeholder="服务名" style="width:150px" /><el-input v-model="externalPager.callType" clearable placeholder="调用类型" style="width:150px;margin-left:8px" /><el-select v-model="externalPager.status" clearable placeholder="状态" style="width:120px;margin-left:8px"><el-option label="成功" value="SUCCESS" /><el-option label="失败" value="FAILED" /></el-select><el-button type="primary" style="margin-left:8px" @click="loadExternalRows">查询</el-button></div></div>
          <AppTable :loading="loading" :error="externalError" :data="externalRows" :total="externalPager.total" :page-no="externalPager.pageNo" :page-size="externalPager.pageSize" :columns="[{ prop: 'createdAt', label: '时间', width: 190 }, { prop: 'serviceName', label: '服务' }, { prop: 'callType', label: '类型' }, { prop: 'status', label: '状态' }, { prop: 'costMs', label: '耗时(ms)', width: 100 }, { prop: 'requestId', label: '请求ID', width: 180 }, { prop: 'errorMessage', label: '错误信息' }]" @page-change="(p, s) => { externalPager.pageNo = p; externalPager.pageSize = s; loadExternalRows(); }">
            <template #empty><EmptyState description="暂无外部调用日志" /></template>
          </AppTable>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>
<style scoped>.filters{display:flex;align-items:center;justify-content:space-between;}</style>
