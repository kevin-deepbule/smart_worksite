<script setup lang="ts">
import { computed } from 'vue';
import type { TaskStageLog } from '../../api/types';

const props = defineProps<{ percentage: number; status?: string; logs?: Array<string | TaskStageLog> }>();
const timeline = computed(() => (props.logs || []).map((log) => typeof log === 'string' ? { message: log, stageName: log, createdAt: '' } : log));
</script>

<template>
  <div class="task-progress">
    <el-progress :percentage="percentage" :status="status === 'FAILED' ? 'exception' : percentage >= 100 ? 'success' : undefined" />
    <el-timeline v-if="timeline.length" style="margin-top: 12px">
      <el-timeline-item v-for="log in timeline" :key="`${log.stageName}-${log.createdAt}`" :timestamp="log.createdAt">
        <strong>{{ log.stageName }}</strong>
        <div class="muted">{{ log.message }}</div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>
