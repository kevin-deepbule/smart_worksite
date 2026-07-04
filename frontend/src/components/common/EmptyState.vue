<script setup lang="ts">
defineProps<{ title?: string; description?: string; actionText?: string }>();
defineEmits<{ action: [] }>();
</script>

<template>
  <el-empty :description="description || '暂无数据'">
    <template #image><div class="empty-mark">∅</div></template>
    <el-button v-if="actionText" type="primary" @click="$emit('action')">{{ actionText }}</el-button>
  </el-empty>
</template>

<style scoped>.empty-mark{width:82px;height:82px;border-radius:24px;display:grid;place-items:center;background:#eef4ff;color:var(--sw-primary);font-size:42px}</style>
