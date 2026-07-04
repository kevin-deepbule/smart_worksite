<script setup lang="ts" generic="T extends Record<string, any>">
withDefaults(defineProps<{
  columns: { prop: string; label: string; width?: number | string; slot?: string }[];
  data: T[];
  loading?: boolean;
  error?: string;
  total?: number;
  pageNo?: number;
  pageSize?: number;
}>(), { total: 0, pageNo: 1, pageSize: 10 });
const emit = defineEmits<{ pageChange: [pageNo: number, pageSize: number] }>();
function onPageChange(page: number, size: number) { emit('pageChange', page, size); }
</script>

<template>
  <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-bottom: 12px" />
  <el-table v-loading="loading" :data="data" border stripe style="width: 100%">
    <template #empty><slot name="empty"><el-empty description="暂无数据" /></slot></template>
    <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width">
      <template v-if="col.slot" #default="scope"><slot :name="col.slot" v-bind="scope" /></template>
    </el-table-column>
    <slot />
  </el-table>
  <div v-if="total > 0" class="pager">
    <el-pagination background layout="total, sizes, prev, pager, next" :total="total" :current-page="pageNo" :page-size="pageSize" @current-change="(page: number) => onPageChange(page, pageSize)" @size-change="(size: number) => onPageChange(1, size)" />
  </div>
</template>

<style scoped>.pager { display: flex; justify-content: flex-end; margin-top: 14px; }</style>
