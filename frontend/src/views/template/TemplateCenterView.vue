<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import AppTable from '../../components/common/AppTable.vue';
import AppUpload from '../../components/common/AppUpload.vue';
import EmptyState from '../../components/common/EmptyState.vue';
import StatusTag from '../../components/common/StatusTag.vue';
import { deleteTemplate, disableTemplate, enableTemplate, fetchTemplateDetail, fetchTemplates, updateTemplate, uploadTemplate, type TemplateItem } from '../../api/template';
import { useProjectStore } from '../../stores/project';
import { useUserStore } from '../../stores/user';
import { hasSuspiciousText } from '../../utils/textQuality';

const projectStore = useProjectStore();
const userStore = useUserStore();
const loading = ref(false);
const saving = ref(false);
const error = ref('');
const rows = ref<TemplateItem[]>([]);
const file = ref<File | null>(null);
const dialogVisible = ref(false);
const form = reactive({ templateId: '', templateName: '', templateCategory: 'REPORT', templateType: 'SAFETY_MONTHLY', scenario: '', versionNo: 'v1.0', description: '' });
const pager = reactive({ pageNo: 1, pageSize: 10, total: 0, templateCategory: '' });
const projectId = computed(() => projectStore.currentProject?.projectId);
const canManageTemplate = computed(() => userStore.hasPermission('file:manage'));
const templateManageTip = '当前账号没有模板维护权限';
const reportTypeOptions = ['SAFETY_MONTHLY', 'QUALITY_WEEKLY', 'GENERAL'];
const reviewTypeOptions = ['SAFETY_REVIEW', 'QUALITY_REVIEW', 'CONTRACT_REVIEW'];
const templateTypeOptions = computed(() => form.templateCategory === 'REPORT' ? reportTypeOptions : reviewTypeOptions);

const templateAccept = computed(() => form.templateCategory === 'REVIEW' ? '.doc,.docx,.pdf,.xls,.xlsx,.csv,.txt,.md' : '.docx,.txt,.md');
const templateTip = computed(() => form.templateCategory === 'REVIEW'
  ? '审查模板按功能清单允许 Word、PDF、Excel/CSV；若后端无法解析会返回明确错误'
  : '报告变量解析当前支持 DOCX、TXT、MD；其他格式需后端模板变量解析扩展');

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
    const page = await fetchTemplates({ pageNo: pager.pageNo, pageSize: pager.pageSize, projectId: projectId.value, templateCategory: pager.templateCategory || undefined });
    rows.value = page.records;
    pager.total = page.total;
  } catch (err) {
    rows.value = [];
    pager.total = 0;
    error.value = err instanceof Error ? err.message : '模板列表加载失败，请检查后端模板接口。';
  } finally { loading.value = false; }
}

function defaultType(category: string) {
  return category === 'REPORT' ? 'SAFETY_MONTHLY' : 'SAFETY_REVIEW';
}

function openCreate(category = 'REPORT') {
  if (!canManageTemplate.value) return ElMessage.warning(templateManageTip);
  Object.assign(form, { templateId: '', templateName: '', templateCategory: category, templateType: defaultType(category), scenario: '', versionNo: 'v1.0', description: '' });
  file.value = null;
  dialogVisible.value = true;
}

async function openEdit(row: TemplateItem) {
  if (!canManageTemplate.value) return ElMessage.warning(templateManageTip);
  try {
    const detail = await fetchTemplateDetail(row.templateId);
    Object.assign(form, { templateId: String(detail.templateId), templateName: detail.templateName, templateCategory: detail.templateCategory || row.templateCategory || 'REPORT', templateType: detail.templateType, scenario: detail.scenario || '', versionNo: detail.versionNo || 'v1.0', description: detail.description || '' });
    file.value = null;
    dialogVisible.value = true;
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '模板详情加载失败，请检查后端模板详情接口。');
  }
}

function onCategoryChange(category: string) {
  form.templateType = defaultType(category);
}

function onFilterChange() {
  pager.pageNo = 1;
  loadRows();
}

function validateForm() {
  if (!projectId.value) return '请先选择项目';
  if (!form.templateCategory) return '请选择模板分类';
  if (!form.templateName.trim()) return '请输入模板名称';
  if (!form.templateType.trim()) return '请输入模板类型';
  if (!form.versionNo.trim()) return '请输入版本号';
  if (!form.templateId && !file.value) return '请选择模板文件';
  return '';
}

function getErrorStatus(error: unknown) {
  return typeof error === 'object' && error !== null && 'response' in error
    ? (error as { response?: { status?: number } }).response?.status
    : undefined;
}

async function save() {
  if (!canManageTemplate.value) return ElMessage.warning(templateManageTip);
  const message = validateForm();
  if (message) return ElMessage.warning(message);
  saving.value = true;
  try {
    if (form.templateId) {
      await updateTemplate(form.templateId, {
        templateName: form.templateName.trim(),
        templateType: form.templateType.trim(),
        scenario: form.scenario.trim() || undefined,
        versionNo: form.versionNo.trim(),
        description: form.description.trim() || undefined
      });
    }
    else {
      await uploadTemplate({
        projectId: projectId.value,
        templateCategory: form.templateCategory,
        templateName: form.templateName.trim(),
        templateType: form.templateType.trim(),
        scenario: form.scenario.trim() || undefined,
        versionNo: form.versionNo.trim(),
        description: form.description.trim() || undefined,
        file: file.value as File
      });
    }
    ElMessage.success('已保存模板');
    dialogVisible.value = false;
    file.value = null;
    await loadRows();
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    const status = getErrorStatus(err);
    if (form.templateId) {
      ElMessage.error(`模板编辑失败，请检查后端模板接口。${detail}`);
    } else if (status === 500) {
      ElMessage.error(`模板上传失败，后端模板接口异常，请查看后端日志；也请检查文件存储配置。${detail}`);
    } else {
      ElMessage.error(`模板上传失败，请检查后端模板接口或文件存储配置。${detail}`);
    }
  } finally { saving.value = false; }
}

async function setStatus(row: TemplateItem, enabled: boolean) {
  if (!canManageTemplate.value) return ElMessage.warning(templateManageTip);
  try {
    enabled ? await enableTemplate(row.templateId) : await disableTemplate(row.templateId);
    ElMessage.success(enabled ? '模板已启用' : '模板已停用');
    await loadRows();
  } catch (err) {
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(enabled ? `模板启用失败，请检查后端模板接口。${detail}` : `模板停用失败，请检查后端模板接口。${detail}`);
  }
}

async function remove(row: TemplateItem) {
  if (!canManageTemplate.value) return ElMessage.warning(templateManageTip);
  try {
    await ElMessageBox.confirm(`确认删除模板 ${row.templateName}？`, '删除模板', { type: 'warning' });
    await deleteTemplate(row.templateId);
    ElMessage.success('模板已删除');
    await loadRows();
  } catch (err) {
    if (err === 'cancel' || err === 'close') return;
    const detail = err instanceof Error && err.message ? ` ${err.message}` : '';
    ElMessage.error(`模板删除失败，请检查后端模板接口。${detail}`);
  }
}
onMounted(async () => { if (!projectStore.currentProject) await projectStore.fetchProjects(); await loadRows(); });
</script>

<template>
  <div class="page">
    <div class="page-header"><div><h2 class="page-title">模板中心</h2><p class="page-desc">报告模板、审查模板统一维护；兼容后续变量提取和启停删除。</p></div><div v-if="canManageTemplate"><el-button @click="openCreate('REVIEW')">上传审查模板</el-button><el-button type="primary" @click="openCreate('REPORT')">上传报告模板</el-button></div></div>
    <el-card class="work-card">
      <template #header><div class="table-head"><strong>模板列表</strong><el-select v-model="pager.templateCategory" clearable placeholder="模板分类" style="width:160px" @change="onFilterChange"><el-option label="报告模板" value="REPORT" /><el-option label="审查模板" value="REVIEW" /></el-select></div></template>
      <AppTable :loading="loading" :error="error" :data="rows" :total="pager.total" :page-no="pager.pageNo" :page-size="pager.pageSize" :columns="[{prop:'templateName',label:'模板名称',slot:'templateName'},{prop:'templateCategory',label:'分类',width:100},{prop:'templateType',label:'类型'},{prop:'versionNo',label:'版本',width:100},{prop:'status',label:'状态',slot:'status',width:110}]" @page-change="(p,s)=>{pager.pageNo=p;pager.pageSize=s;loadRows()}">
        <template #empty><EmptyState description="暂无模板，可先上传报告模板或审查模板。" /></template>
        <template #templateName="{ row }">
          <span>{{ row.templateName }}</span>
          <el-tag v-if="hasSuspiciousText(row.templateName)" type="warning" size="small" style="margin-left: 6px">疑似历史乱码数据</el-tag>
        </template>
        <template #status="{ row }"><StatusTag :status="row.status" :text="row.status === 'ACTIVE' ? 'ENABLED' : row.status" /></template>
        <el-table-column label="操作" width="220"><template #default="{ row }"><el-button link @click="openEdit(row)">编辑</el-button><el-button link type="primary" @click="setStatus(row, row.status === 'DISABLED')">{{ row.status === 'DISABLED' ? '启用' : '停用' }}</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
      </AppTable>
    </el-card>
    <el-dialog v-model="dialogVisible" title="模板信息" width="620px">
      <el-form label-width="96px">
        <el-form-item label="模板名称" required><el-input v-model="form.templateName" /></el-form-item>
        <el-form-item label="模板分类" required><el-select v-model="form.templateCategory" :disabled="!!form.templateId" @change="onCategoryChange"><el-option label="报告模板" value="REPORT" /><el-option label="审查模板" value="REVIEW" /></el-select></el-form-item>
        <el-form-item label="模板类型" required>
          <el-select v-model="form.templateType" filterable allow-create default-first-option>
            <el-option v-for="item in templateTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用场景"><el-input v-model="form.scenario" /></el-form-item>
        <el-form-item label="版本号" required><el-input v-model="form.versionNo" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item v-if="!form.templateId" label="模板文件" required><AppUpload :model-value="file ? [file] : []" :accept="templateAccept" :multiple="false" :max-size-mb="50" :tip="templateTip" @update:model-value="file = $event[0] || null" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>.table-head { display:flex; justify-content:space-between; align-items:center; }</style>
