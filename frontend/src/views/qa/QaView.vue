<script setup lang="ts">
import { onMounted, ref } from 'vue';
import JsonViewer from '../../components/common/JsonViewer.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { createQaSession, fetchQaMessages, fetchQaSessions, sendQuestion } from '../../api/qa';
import { useProjectStore } from '../../stores/project';
import type { QaMessage, QaSession } from '../../api/types';

const projectStore = useProjectStore();
const loading = ref(false);
const error = ref('');
const question = ref('临边洞口防护有哪些验收要点？');
const sessions = ref<QaSession[]>([]);
const activeSessionId = ref<string | number>('');
const messages = ref<QaMessage[]>([]);
const references = ref<QaMessage['references']>([]);

async function loadData() {
  loading.value = true;
  error.value = '';
  try {
    if (!projectStore.currentProject) await projectStore.fetchProjects();
    sessions.value = await fetchQaSessions(projectStore.currentProject?.projectId || 0);
    if (!sessions.value.length) sessions.value = [await createQaSession({ projectId: projectStore.currentProject?.projectId || 0, title: '默认会话' })];
    activeSessionId.value = sessions.value[0].sessionId;
    messages.value = await fetchQaMessages(activeSessionId.value);
    references.value = messages.value.find((item) => item.references?.length)?.references || [];
  } catch (err) {
    error.value = err instanceof Error ? err.message : '问答数据加载失败';
  } finally { loading.value = false; }
}

async function ask() {
  if (!question.value.trim()) return;
  loading.value = true;
  error.value = '';
  try {
    const userMessage: QaMessage = { id: Date.now(), messageId: Date.now(), sessionId: activeSessionId.value, projectId: projectStore.currentProject?.projectId || 0, taskId: 0, fileId: 0, role: 'user', content: question.value, question: question.value, status: 'SUCCESS', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
    messages.value.push(userMessage);
    const answer = await sendQuestion(activeSessionId.value, { projectId: projectStore.currentProject?.projectId || 0, question: question.value, routeMode: 'MIXED' });
    messages.value.push(answer);
    references.value = answer.references || [];
    question.value = '';
  } catch (err) {
    error.value = err instanceof Error ? err.message : '发送问题失败';
  } finally { loading.value = false; }
}

onMounted(loadData);
</script>

<template>
  <div class="page"><el-alert v-if="error" :title="error" type="error" show-icon /><div class="page-header"><div><h2 class="page-title">知识问答</h2><p class="page-desc">支持连续提问、来源引用、数据库结果和答案反馈。</p></div></div><div class="three-col"><el-card class="work-card"><h3 class="panel-title">会话列表</h3><EmptyState v-if="!sessions.length" description="暂无会话" /><el-menu v-else v-model="activeSessionId"><el-menu-item v-for="item in sessions" :key="item.sessionId" :index="String(item.sessionId)">{{ item.title }}</el-menu-item></el-menu></el-card><el-card class="work-card" v-loading="loading"><h3 class="panel-title">对话区</h3><EmptyState v-if="!messages.length" description="暂无消息" /><div v-for="msg in messages" :key="msg.messageId" class="chat"><b>{{ msg.role === 'user' ? '我' : 'AI助手' }}</b><p>{{ msg.content }}</p></div><el-input v-model="question" type="textarea" :rows="3" placeholder="输入问题" /><el-button type="primary" style="margin-top:10px" :loading="loading" @click="ask">发送</el-button></el-card><el-card class="work-card"><h3 class="panel-title">来源引用</h3><EmptyState v-if="!references?.length" description="暂无引用" /><div v-for="source in references" :key="`${source.title}-${source.page}`" class="source"><b>{{ source.title }}</b><p class="muted">{{ source.page }} · 置信度 {{ source.score }}</p></div><JsonViewer :value="{ route:'MIXED', topK:references || [] }" title="检索Trace" /></el-card></div></div>
</template>
<style scoped>.chat{padding:12px;border:1px solid var(--sw-border);border-radius:12px;margin-bottom:10px}.chat p{margin:6px 0 0}.source{padding:10px;border-bottom:1px solid var(--sw-border);margin-bottom:10px}</style>
