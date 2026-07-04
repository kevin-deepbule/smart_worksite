<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../../stores/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const loading = ref(false);
const form = reactive({ username: 'admin', password: 'admin123' });

async function submit() {
  loading.value = true;
  try {
    await userStore.login(form);
    ElMessage.success('登录成功');
    router.replace((route.query.redirect as string) || '/dashboard');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败');
  } finally { loading.value = false; }
}
</script>

<template>
  <div class="login-page">
    <div class="skyline"></div>
    <el-card class="login-card">
      <h1>智慧工地大模型应用系统</h1>
      <p>项目知识库 · 智能问答 · 合规审查 · 报告生成 · OCR识别</p>
      <el-form :model="form" label-position="top" @keyup.enter="submit">
        <el-form-item label="账号"><el-input v-model="form.username" size="large" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password size="large" /></el-form-item>
        <el-button type="primary" size="large" :loading="loading" style="width:100%" @click="submit">登录</el-button>
      </el-form>
      <div class="hint">开发阶段默认 mock 登录，后续联调 /api/auth/login。</div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page{min-height:100vh;display:grid;place-items:center;background:linear-gradient(135deg,#eaf2ff,#f8fafc 45%,#ecfeff);position:relative;overflow:hidden}.login-page:before{content:"";position:absolute;inset:0;background-image:linear-gradient(#cbd5e1 1px,transparent 1px),linear-gradient(90deg,#cbd5e1 1px,transparent 1px);background-size:42px 42px;opacity:.35}.skyline{position:absolute;bottom:0;width:100%;height:180px;background:linear-gradient(transparent,#dbeafe)}.login-card{width:430px;z-index:1;border-radius:20px}.login-card h1{margin:0 0 10px;font-size:26px}.login-card p{margin:0 0 26px;color:var(--sw-muted)}.hint{margin-top:16px;color:var(--sw-muted);font-size:12px;text-align:center}
</style>

