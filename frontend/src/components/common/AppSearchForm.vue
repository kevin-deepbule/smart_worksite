<script setup lang="ts">
type SearchField = { prop: string; label: string; type?: 'input' | 'select' | 'date'; options?: { label: string; value: string }[]; placeholder?: string };
const props = defineProps<{ modelValue: Record<string, unknown>; fields: SearchField[] }>();
const emit = defineEmits<{ 'update:modelValue': [value: Record<string, unknown>]; search: []; reset: [] }>();
function patch(prop: string, value: unknown) { emit('update:modelValue', { ...props.modelValue, [prop]: value }); }
</script>

<template>
  <el-form :inline="true" :model="modelValue" class="search-form">
    <el-form-item v-for="field in fields" :key="field.prop" :label="field.label">
      <el-select v-if="field.type === 'select'" :model-value="modelValue[field.prop]" :placeholder="field.placeholder || '请选择'" clearable style="width: 180px" @update:model-value="(value: unknown) => patch(field.prop, value)">
        <el-option v-for="item in field.options" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-date-picker v-else-if="field.type === 'date'" :model-value="modelValue[field.prop]" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" @update:model-value="(value: unknown) => patch(field.prop, value)" />
      <el-input v-else :model-value="modelValue[field.prop]" :placeholder="field.placeholder || '请输入'" clearable @update:model-value="(value: unknown) => patch(field.prop, value)" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="emit('search')">查询</el-button>
      <el-button @click="emit('reset')">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<style scoped>.search-form { padding: 14px 14px 0; background: #fff; border: 1px solid var(--sw-border); border-radius: 14px; }</style>
