<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as memberApi from '../../api/member';
import * as userApi from '../../api/user';
import type { ProjectMemberItem, UserItem } from '../../api/types';
import { useProjectStore } from '../../stores/project';

const projectStore = useProjectStore();
const projectId = ref<string | number>(projectStore.currentProjectId || '');
const members = ref<ProjectMemberItem[]>([]);
const allUsers = ref<UserItem[]>([]);
const loading = ref(false);
const userLoading = ref(false);
const submitting = ref(false);

const dialogVisible = ref(false);
const editingUserId = ref<number | string | null>(null);
const form = ref({ userId: '' as string | number, projectRole: 'BUSINESS_USER' });

const PROJECT_ROLES = [
  { value: 'PROJECT_ADMIN', label: '项目管理员' },
  { value: 'BUSINESS_USER', label: '业务人员' },
  { value: 'VIEWER', label: '只读用户' }
];

const roleLabel = (role: string) => PROJECT_ROLES.find((item) => item.value === role)?.label || role;
const existingUserIds = computed(() => new Set(members.value.map((member) => String(member.userId))));
const availableUsers = computed(() => allUsers.value.filter((user) => user.status === 'ENABLED' && !existingUserIds.value.has(String(user.id))));

async function fetchMembers() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    members.value = await memberApi.listMembers(projectId.value);
  } finally {
    loading.value = false;
  }
}

async function fetchUsers() {
  userLoading.value = true;
  try {
    const res = await userApi.listUsers({ pageNo: 1, pageSize: 100, status: 'ENABLED' });
    allUsers.value = res.records;
  } finally {
    userLoading.value = false;
  }
}

onMounted(async () => {
  await Promise.all([fetchMembers(), fetchUsers()]);
});

async function openAdd() {
  editingUserId.value = null;
  form.value = { userId: '', projectRole: 'BUSINESS_USER' };
  dialogVisible.value = true;
  await Promise.all([fetchMembers(), fetchUsers()]);
}

function openEdit(row: ProjectMemberItem) {
  editingUserId.value = row.userId;
  form.value = { userId: row.userId, projectRole: row.projectRole };
  dialogVisible.value = true;
}

async function submit() {
  if (!projectId.value) {
    ElMessage.error('请先选择项目');
    return;
  }
  if (!form.value.userId && editingUserId.value == null) {
    ElMessage.error('请选择用户');
    return;
  }
  submitting.value = true;
  try {
    if (editingUserId.value == null) {
      await memberApi.addMember(projectId.value, { userId: form.value.userId, projectRole: form.value.projectRole });
      ElMessage.success('成员添加成功');
    } else {
      await memberApi.updateMember(projectId.value, editingUserId.value, { userId: editingUserId.value, projectRole: form.value.projectRole });
      ElMessage.success('角色更新成功');
    }
    dialogVisible.value = false;
    await fetchMembers();
  } finally {
    submitting.value = false;
  }
}

async function remove(row: ProjectMemberItem) {
  await ElMessageBox.confirm(`确认移除成员“${row.displayName || row.username}”？`, '提示', { type: 'warning' });
  await memberApi.removeMember(projectId.value, row.userId);
  ElMessage.success('已移除');
  await fetchMembers();
}
</script>

<template>
  <div>
    <div class="page-header">
      <h2>项目成员管理</h2>
      <el-button type="primary" @click="openAdd">添加成员</el-button>
    </div>

    <el-table :data="members" v-loading="loading" stripe>
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="displayName" label="显示名称" width="160" />
      <el-table-column label="项目角色" width="140">
        <template #default="{ row }">
          <el-tag size="small" :type="row.projectRole === 'PROJECT_ADMIN' ? 'primary' : row.projectRole === 'VIEWER' ? 'info' : 'success'">
            {{ roleLabel(row.projectRole) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ENABLED' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="加入时间" width="180">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').slice(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">修改角色</el-button>
          <el-button link type="danger" @click="remove(row)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingUserId == null ? '添加成员' : '修改角色'" width="400px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item v-if="editingUserId == null" label="选择用户">
          <el-select v-model="form.userId" filterable :loading="userLoading" style="width: 100%" placeholder="请选择用户">
            <el-option v-for="user in availableUsers" :key="user.id" :label="`${user.displayName} (${user.username})`" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目角色">
          <el-select v-model="form.projectRole" style="width: 100%">
            <el-option v-for="role in PROJECT_ROLES" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
}
</style>
