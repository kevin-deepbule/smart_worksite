import type { RouteRecordRaw } from 'vue-router';
import { createRouter, createWebHistory } from 'vue-router';
import MainLayout from '../layouts/MainLayout.vue';
import { useUserStore } from '../stores/user';

export const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('../views/login/LoginView.vue'), meta: { public: true } },
  { path: '/', component: MainLayout, redirect: '/dashboard', children: [
    { path: 'dashboard', name: 'dashboard', component: () => import('../views/dashboard/DashboardView.vue'), meta: { title: '首页工作台', permission: 'dashboard:view' } },
    { path: 'knowledge', name: 'knowledge', component: () => import('../views/knowledge/KnowledgeBaseView.vue'), meta: { title: '知识库管理', permission: 'knowledge:view' } },
    { path: 'qa', name: 'qa', component: () => import('../views/qa/QaView.vue'), meta: { title: '知识问答', permission: 'qa:view' } },
    { path: 'review', name: 'review', component: () => import('../views/review/ComplianceReviewView.vue'), meta: { title: '合规审查', permission: 'review:view' } },
    { path: 'report', name: 'report', component: () => import('../views/report/ReportListView.vue'), meta: { title: '报告管理', permission: 'report:view' } },
    { path: 'report/:id', name: 'reportDetail', component: () => import('../views/report/ReportDetailView.vue'), meta: { title: '报告详情', permission: 'report:view' } },
    { path: 'ocr', name: 'ocr', component: () => import('../views/ocr/OcrView.vue'), meta: { title: 'OCR识别', permission: 'ocr:view' } },
    { path: '403', name: 'forbidden', component: () => import('../views/error/ForbiddenView.vue'), meta: { title: '无权限' } }
  ] },
  { path: '/:pathMatch(.*)*', name: 'notFound', component: () => import('../views/error/NotFoundView.vue'), meta: { public: true } }
];

export const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const userStore = useUserStore();
  if (!to.meta.public && !userStore.isLoggedIn) return { path: '/login', query: { redirect: to.fullPath } };
  if (!userStore.hasPermission(to.meta.permission as string | undefined)) return '/403';
  return true;
});
