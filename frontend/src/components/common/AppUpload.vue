<script setup lang="ts">
import { ref } from 'vue';
import { Loading } from '@element-plus/icons-vue';
import { ElMessage, type UploadFile, type UploadFiles, type UploadProps, type UploadUserFile } from 'element-plus';

const props = withDefaults(defineProps<{
  accept?: string;
  maxSizeMb?: number;
  tip?: string;
  disabled?: boolean;
  uploading?: boolean;
  error?: string;
}>(), { maxSizeMb: 20, tip: '支持 Word、PDF、Excel、PPT、图片等资料' });

const emit = defineEmits<{ change: [files: File[]] }>();
const fileList = ref<UploadUserFile[]>([]);

function parseAccept() {
  return (props.accept || '').split(',').map((item) => item.trim().toLowerCase()).filter(Boolean);
}

function isAllowedType(file: File) {
  const rules = parseAccept();
  if (!rules.length) return true;
  const name = file.name.toLowerCase();
  const mime = file.type.toLowerCase();
  return rules.some((rule) => {
    if (rule.startsWith('.')) return name.endsWith(rule);
    if (rule.endsWith('/*')) return mime.startsWith(rule.slice(0, -1));
    return mime === rule;
  });
}

function validateFile(file: File) {
  if (file.size / 1024 / 1024 > props.maxSizeMb) return `文件 ${file.name} 超过 ${props.maxSizeMb}MB`;
  if (!isAllowedType(file)) return `文件 ${file.name} 类型不符合要求`;
  return '';
}

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const message = validateFile(rawFile);
  if (message) {
    ElMessage.error(message);
    return false;
  }
  return true;
};

function onChange(_file: UploadFile, files: UploadFiles) {
  const validItems: UploadUserFile[] = [];
  const validRawFiles: File[] = [];
  for (const item of files) {
    if (!item.raw) continue;
    const message = validateFile(item.raw);
    if (message) {
      ElMessage.error(message);
      continue;
    }
    validItems.push(item);
    validRawFiles.push(item.raw);
  }
  fileList.value = validItems;
  emit('change', validRawFiles);
}
</script>

<template>
  <div>
    <el-upload v-model:file-list="fileList" drag multiple :auto-upload="false" :accept="accept" :disabled="disabled || uploading" :before-upload="beforeUpload" @change="onChange">
      <div class="upload-icon">+</div>
      <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
      <template #tip><div class="el-upload__tip">{{ tip }}，单文件不超过 {{ maxSizeMb }}MB</div></template>
    </el-upload>
    <div v-if="uploading" class="upload-state"><el-icon class="is-loading"><Loading /></el-icon> 上传处理中...</div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-top: 10px" />
  </div>
</template>

<style scoped>
.upload-icon { font-size: 34px; color: var(--sw-primary); line-height: 1; }
.upload-state { margin-top: 10px; color: var(--sw-muted); display: flex; align-items: center; gap: 6px; }
</style>

