<script setup lang="ts">
const props = defineProps<{ status?: string; text?: string }>();
const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = { ACTIVE: 'success', SUCCESS: 'success', DONE: 'success', PROCESSING: 'warning', PENDING: 'warning', FAILED: 'danger', ERROR: 'danger', DISABLED: 'info', ARCHIVED: 'info' };
</script>

<template><el-tag :type="map[props.status || ''] || 'info'">{{ text || status || '未知' }}</el-tag></template>
