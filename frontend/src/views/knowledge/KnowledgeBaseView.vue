<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AppUpload from '../../components/common/AppUpload.vue';
import AppTable from '../../components/common/AppTable.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { fetchKnowledgeBases, fetchKnowledgeDocuments } from '../../api/knowledge';
import { useProjectStore } from '../../stores/project';
import type { KnowledgeBase, KnowledgeDocument } from '../../api/types';

const projectStore = useProjectStore();
const loading = ref(false);
const error = ref('');
const bases = ref<KnowledgeBase[]>([]);
const docs = ref<KnowledgeDocument[]>([]);

async function loadData() {
  loading.value = true;
  error.value = '';
  try {
    if (!projectStore.currentProject) await projectStore.fetchProjects();
    bases.value = await fetchKnowledgeBases(projectStore.currentProject?.projectId || 0);
    docs.value = bases.value[0] ? (await fetchKnowledgeDocuments(bases.value[0].id)).records : [];
  } catch (err) {
    error.value = err instanceof Error ? err.message : '知识库数据加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <div class="page" v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon />
    <div class="page-header"><div><h2 class="page-title">知识库管理</h2><p class="page-desc">项目级知识库、文档解析状态和入库进度。</p></div><el-button type="primary">新建知识库</el-button></div>
    <EmptyState v-if="!loading && !error && !bases.length" description="暂无知识库" action-text="创建知识库" />
    <template v-else>
      <div class="two-col"><el-card v-for="base in bases" :key="base.id" class="work-card"><h3>{{ base.name }}</h3><p class="muted">{{ base.description }}</p><p class="muted">文档数量：{{ base.documentCount }}</p><StatusTag :status="base.status" /></el-card></div>
      <el-card class="work-card"><h3 class="panel-title">上传文档</h3><AppUpload accept=".doc,.docx,.pdf,.xls,.xlsx,.ppt,.pptx,.jpg,.jpeg,.png" /></el-card>
      <el-card class="work-card"><h3 class="panel-title">文档处理状态</h3><AppTable :data="docs" :columns="[{prop:'fileName',label:'文档名称'},{prop:'parseStatus',label:'解析状态',slot:'parse'},{prop:'indexStatus',label:'入库状态',slot:'index'},{prop:'failReason',label:'说明'}]"><template #empty><EmptyState description="暂无知识库文档" /></template><template #parse="{ row }"><StatusTag :status="row.parseStatus" /></template><template #index="{ row }"><StatusTag :status="row.indexStatus" /></template></AppTable></el-card>
    </template>
  </div>
</template>
