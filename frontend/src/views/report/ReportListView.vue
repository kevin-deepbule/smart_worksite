<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import AppSearchForm from '../../components/common/AppSearchForm.vue';
import AppTable from '../../components/common/AppTable.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { fetchReports } from '../../api/report';
import { useProjectStore } from '../../stores/project';
import type { ReportItem } from '../../api/types';

const projectStore = useProjectStore();
const loading = ref(false);
const error = ref('');
const search = reactive({ keyword: '', status: '' });
const reports = ref<ReportItem[]>([]);

async function loadData() {
  loading.value = true;
  error.value = '';
  try {
    if (!projectStore.currentProject) await projectStore.fetchProjects();
    const page = await fetchReports({ projectId: projectStore.currentProject?.projectId, keyword: search.keyword, status: search.status, pageNo: 1, pageSize: 20 });
    reports.value = page.records;
  } catch (err) { error.value = err instanceof Error ? err.message : '报告列表加载失败'; }
  finally { loading.value = false; }
}
function reset() { search.keyword = ''; search.status = ''; loadData(); }
onMounted(loadData);
</script>

<template>
  <div class="page"><el-alert v-if="error" :title="error" type="error" show-icon /><div class="page-header"><div><h2 class="page-title">报告管理</h2><p class="page-desc">报告列表、版本、预览、下载、重新生成和归档。</p></div><el-button type="primary">新建报告</el-button></div><AppSearchForm v-model="search" :fields="[{prop:'keyword',label:'关键词'},{prop:'status',label:'状态',type:'select',options:[{label:'成功',value:'SUCCESS'},{label:'处理中',value:'PROCESSING'},{label:'失败',value:'FAILED'}]}]" @search="loadData" @reset="reset" /><el-card class="work-card"><AppTable :loading="loading" :data="reports" :error="error" :total="reports.length" :columns="[{prop:'reportName',label:'报告名称'},{prop:'version',label:'版本'},{prop:'status',label:'状态',slot:'status'},{prop:'createdBy',label:'创建人'},{prop:'updatedAt',label:'更新时间'}]"><template #empty><EmptyState description="暂无报告" /></template><template #status="{ row }"><StatusTag :status="row.status" /></template><el-table-column label="操作" width="180"><template #default="{ row }"><el-button link type="primary" @click="$router.push(`/report/${row.reportId}`)">详情</el-button><el-button link>下载</el-button></template></el-table-column></AppTable></el-card></div>
</template>
