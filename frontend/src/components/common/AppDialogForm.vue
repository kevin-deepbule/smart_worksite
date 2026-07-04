<script setup lang="ts">
defineProps<{ modelValue: boolean; title: string; loading?: boolean; width?: string }>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; submit: [] }>();
</script>

<template>
  <el-dialog :model-value="modelValue" :title="title" :width="width || '520px'" @update:model-value="emit('update:modelValue', $event)">
    <slot />
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="emit('submit')">提交</el-button>
    </template>
  </el-dialog>
</template>
