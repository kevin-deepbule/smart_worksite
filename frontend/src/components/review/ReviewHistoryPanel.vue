<script setup lang="ts">
import { ref, watch } from 'vue';
import { fetchReviewRecords } from '../../api/review';
import type { ID, ReviewRecord } from '../../api/types';
import AppTable from '../common/AppTable.vue';
import EmptyState from '../common/EmptyState.vue';
import StatusTag from '../common/StatusTag.vue';

const props = withDefaults(defineProps<{
  projectId?: ID;
  activeRecordId?: ID;
  refreshKey?: number;
}>(), {
  projectId: '',
  activeRecordId: '',
  refreshKey: 0
});

const emit = defineEmits<{
  select: [record: ReviewRecord];
}>();

const loading = ref(false);
const error = ref('');
const rows = ref<ReviewRecord[]>([]);
const status = ref('');
const pager = ref({ pageNo: 1, pageSize: 10, total: 0 });
let requestSequence = 0;

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELED' },
  { label: '已归档', value: 'ARCHIVED' }
];

const columns = [
  { prop: 'recordId', label: '记录 ID', width: 100 },
  { prop: 'templateId', label: '模板 ID', width: 100 },
  { prop: 'status', label: '审核状态', width: 110, slot: 'status' },
  { prop: 'overallStatus', label: '审核结论', width: 120, slot: 'overallStatus' },
  { prop: 'progress', label: '进度', width: 90, slot: 'progress' },
  { prop: 'summary', label: '结果摘要', slot: 'summary' },
  { prop: 'createdAt', label: '发起时间', width: 180, slot: 'createdAt' }
];

function progressOf(record: ReviewRecord) {
  if (typeof record.progress === 'number') return record.progress;
  return ['COMPLETED', 'FAILED', 'CANCELED', 'ARCHIVED'].includes(String(record.status)) ? 100 : 0;
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}

function isActive(recordId: ID) {
  return String(recordId) === String(props.activeRecordId || '');
}

async function loadRecords(resetPage = false) {
  if (resetPage) pager.value.pageNo = 1;
  if (!props.projectId) {
    rows.value = [];
    pager.value.total = 0;
    error.value = '';
    return;
  }
  const currentSequence = ++requestSequence;
  loading.value = true;
  error.value = '';
  try {
    const page = await fetchReviewRecords({
      projectId: props.projectId,
      status: status.value || undefined,
      pageNo: pager.value.pageNo,
      pageSize: pager.value.pageSize
    });
    if (currentSequence !== requestSequence) return;
    rows.value = page.records;
    pager.value = {
      pageNo: page.pageNo,
      pageSize: page.pageSize,
      total: page.total
    };
  } catch (err) {
    if (currentSequence !== requestSequence) return;
    rows.value = [];
    pager.value.total = 0;
    error.value = err instanceof Error ? err.message : '审核历史记录加载失败';
  } finally {
    if (currentSequence === requestSequence) loading.value = false;
  }
}

function changePage(pageNo: number, pageSize: number) {
  pager.value.pageNo = pageNo;
  pager.value.pageSize = pageSize;
  void loadRecords();
}

watch(
  () => [String(props.projectId || ''), props.refreshKey],
  () => void loadRecords(true),
  { immediate: true }
);
</script>

<template>
  <el-card class="work-card history-card">
    <template #header>
      <div class="history-header">
        <div>
          <strong>审核历史记录</strong>
          <p>选择历史记录可以查看审核结论、规则结果、问题列表和执行阶段。</p>
        </div>
        <div class="history-actions">
          <el-select v-model="status" style="width: 130px" @change="loadRecords(true)">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button :loading="loading" @click="loadRecords(false)">刷新</el-button>
        </div>
      </div>
    </template>

    <AppTable
      :data="rows"
      :columns="columns"
      :loading="loading"
      :error="error"
      :total="pager.total"
      :page-no="pager.pageNo"
      :page-size="pager.pageSize"
      max-height="440"
      @page-change="changePage"
    >
      <template #empty>
        <EmptyState description="当前项目暂无审核历史记录。" />
      </template>
      <template #status="{ row }">
        <StatusTag :status="row.status" />
      </template>
      <template #overallStatus="{ row }">
        <StatusTag v-if="row.overallStatus" :status="row.overallStatus" />
        <span v-else>-</span>
      </template>
      <template #progress="{ row }">
        {{ progressOf(row) }}%
      </template>
      <template #summary="{ row }">
        <span class="summary-text" :title="row.summary || row.errorMessage || ''">
          {{ row.summary || row.errorMessage || '暂无结果摘要' }}
        </span>
      </template>
      <template #createdAt="{ row }">
        {{ formatTime(row.createdAt) }}
      </template>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="isActive(row.recordId)"
            @click="emit('select', row)"
          >
            {{ isActive(row.recordId) ? '正在查看' : '查看结果' }}
          </el-button>
        </template>
      </el-table-column>
    </AppTable>
  </el-card>
</template>

<style scoped>
.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.history-header p {
  margin: 6px 0 0;
  color: var(--sw-muted);
  font-size: 13px;
  font-weight: 400;
}
.history-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.summary-text {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-height: 1.5;
}
@media (max-width: 760px) {
  .history-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .history-actions {
    width: 100%;
  }
}
</style>
