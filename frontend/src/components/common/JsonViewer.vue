<script setup lang="ts">
import { computed } from 'vue';
import { ElMessage } from 'element-plus';
const props = defineProps<{ value: unknown; title?: string }>();
const jsonText = computed(() => JSON.stringify(props.value, null, 2));
async function copy() { await navigator.clipboard.writeText(jsonText.value); ElMessage.success('JSON已复制'); }
function download() { const blob = new Blob([jsonText.value], { type: 'application/json' }); const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'result.json'; a.click(); URL.revokeObjectURL(a.href); }
</script>

<template>
  <div class="json-viewer">
    <div class="json-head"><strong>{{ title || 'JSON结果' }}</strong><span><el-button size="small" @click="copy">复制</el-button><el-button size="small" @click="download">下载</el-button></span></div>
    <pre>{{ jsonText }}</pre>
  </div>
</template>

<style scoped>.json-viewer{border:1px solid var(--sw-border);border-radius:12px;overflow:hidden;background:#0f172a}.json-head{display:flex;justify-content:space-between;align-items:center;padding:10px 12px;background:#fff}.json-viewer pre{margin:0;padding:14px;color:#dbeafe;overflow:auto;max-height:360px}</style>
