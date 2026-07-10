<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import AppTable from '../../components/common/AppTable.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import { createProject, fetchProjects, updateProject, updateProjectStatus } from '../../api/project';
import { listMembers } from '../../api/member';
import type { ProjectItem, ProjectMemberItem } from '../../api/types';
import { useProjectStore } from '../../stores/project';

const projectStore = useProjectStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const projects = ref<ProjectItem[]>([]);
const members = ref<ProjectMemberItem[]>([]);
const activeProject = ref<ProjectItem | null>(null);
const dialogVisible = ref(false);
const form = reactive({ projectId: '', name: '', code: '', address: '', description: '' });

async function loadProjects() {
  loading.value = true;
  error.value = '';
  try {
    const page = await fetchProjects({ pageNo: 1, pageSize: 100 });
    projects.value = page.records;
    if (!projects.value.length) {
      activeProject.value = null;
      members.value = [];
      projectStore.switchProject('');
      return;
    }
    const current = projects.value.find((item) => String(item.projectId) === String(projectStore.currentProjectId)) || projects.value[0];
    if (!activeProject.value || !projects.value.some((item) => String(item.projectId) === String(activeProject.value?.projectId))) {
      await selectProject(current);
    } else {
      activeProject.value = projects.value.find((item) => String(item.projectId) === String(activeProject.value?.projectId)) || current;
    }
    projectStore.projects = projects.value;
    if (!projectStore.currentProjectId || !projects.value.some((item) => String(item.projectId) === String(projectStore.currentProjectId))) {
      projectStore.switchProject(current.projectId);
    }
  } catch (err) {
    projects.value = [];
    members.value = [];
    error.value = err instanceof Error ? err.message : '项目列表加载失败，请检查后端项目接口。';
  } finally { loading.value = false; }
}

async function selectProject(project: ProjectItem) {
  activeProject.value = project;
  projectStore.projects = projects.value;
  projectStore.switchProject(project.projectId);
  try { members.value = await listMembers(project.projectId); }
  catch { members.value = []; }
}

function openCreate() {
  Object.assign(form, { projectId: '', name: '', code: '', address: '', description: '' });
  dialogVisible.value = true;
}

function openEdit(row: ProjectItem) {
  Object.assign(form, { projectId: String(row.projectId), name: row.name, code: row.code, address: row.address, description: row.description || '' });
  dialogVisible.value = true;
}

async function saveProject() {
  if (!form.name.trim()) return ElMessage.warning('请输入项目名称');
  if (!form.code.trim()) return ElMessage.warning('请输入项目编码');
  saving.value = true;
  try {
    let saved: ProjectItem;
    if (form.projectId) saved = await updateProject(form.projectId, form);
    else saved = await createProject(form);
    ElMessage.success('已保存项目');
    dialogVisible.value = false;
    await loadProjects();
    if (!form.projectId) await selectProject(saved);
    if (form.projectId && String(projectStore.currentProjectId) === String(form.projectId)) projectStore.switchProject(form.projectId);
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(form.projectId ? `项目更新失败，请检查后端项目接口。${detail}` : `项目创建失败，请检查后端项目接口。${detail}`);
  } finally { saving.value = false; }
}

async function toggleProjectStatus(row: ProjectItem) {
  const enabled = ['ACTIVE', 'ENABLED'].includes(row.status);
  const nextStatus = enabled ? 'DISABLED' : 'ENABLED';
  const actionText = enabled ? '停用' : '启用';
  await ElMessageBox.confirm(`确认${actionText}项目“${row.name || row.projectName}”？`, `${actionText}项目`, { type: 'warning' });
  try {
    await updateProjectStatus(row.projectId, nextStatus);
    ElMessage.success(`项目已${actionText}`);
    await loadProjects();
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`项目${actionText}失败，请检查后端项目状态接口。${detail}`);
  }
}

onMounted(loadProjects);
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div><h2 class="page-title">项目与权限</h2><p class="page-desc">项目档案、成员角色、项目级权限隔离。</p></div>
      <el-button type="primary" @click="openCreate">新建项目</el-button>
    </div>
    <div class="two-col">
      <el-card class="work-card">
        <template #header><strong>项目列表</strong></template>
        <AppTable :loading="loading" :error="error" :data="projects" :columns="[{prop:'name',label:'项目名称'},{prop:'code',label:'编码'},{prop:'status',label:'状态',slot:'status'}]">
          <template #empty><EmptyState description="暂无项目，请先创建项目。" /></template>
          <template #status="{ row }"><StatusTag :status="row.status" /></template>
          <el-table-column label="操作" width="210">
            <template #default="{ row }">
              <el-button link type="primary" @click="selectProject(row)">成员</el-button>
              <el-button link @click="openEdit(row)">编辑</el-button>
              <el-button link :type="['ACTIVE', 'ENABLED'].includes(row.status) ? 'danger' : 'success'" @click="toggleProjectStatus(row)">
                {{ ['ACTIVE', 'ENABLED'].includes(row.status) ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </AppTable>
      </el-card>
      <el-card class="work-card">
        <template #header>
          <div class="member-head">
            <strong>{{ activeProject?.name || '项目成员' }}</strong>
            <el-button size="small" @click="router.push('/project/members')">成员管理</el-button>
          </div>
        </template>
        <AppTable :data="members" :columns="[{prop:'displayName',label:'成员'},{prop:'projectRole',label:'角色'},{prop:'status',label:'状态',slot:'status'}]">
          <template #empty><EmptyState description="暂无项目成员。" /></template>
          <template #status="{ row }"><StatusTag :status="row.status" /></template>
        </AppTable>
      </el-card>
    </div>
    <el-dialog v-model="dialogVisible" title="项目档案" width="560px">
      <el-form label-width="92px">
        <el-form-item label="项目名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="项目编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="项目地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveProject">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.member-head { display: flex; justify-content: space-between; align-items: center; }
</style>
