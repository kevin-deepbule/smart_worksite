import type { RouteRecordRaw } from 'vue-router';
import { createRouter, createWebHistory } from 'vue-router';
import MainLayout from '../layouts/MainLayout.vue';
import { useUserStore } from '../stores/user';

export const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('../views/login/LoginView.vue'), meta: { public: true } },
  { path: '/', component: MainLayout, redirect: '/dashboard', children: [
    { path: 'dashboard', name: 'dashboard', component: () => import('../views/dashboard/DashboardView.vue'), meta: { title: '工作台', permission: 'dashboard:view' } },
    { path: 'projects', name: 'projects', component: () => import('../views/project/ProjectManagementView.vue'), meta: { title: '项目管理', permission: 'project:view' } },
    { path: 'project', redirect: '/projects' },
    { path: 'files', name: 'files', component: () => import('../views/file/FileManagementView.vue'), meta: { title: '文件管理', permission: 'file:view' } },
    { path: 'templates', name: 'templates', component: () => import('../views/template/TemplateCenterView.vue'), meta: { title: '模板中心', permission: 'template:view' } },
    { path: 'knowledge', name: 'knowledge', component: () => import('../views/knowledge/KnowledgeBaseView.vue'), meta: { title: '知识库', permission: 'knowledge:view' } },
    { path: 'qa', name: 'qa', component: () => import('../views/qa/QaView.vue'), meta: { title: '智能问答', permission: 'qa:view' } },
    { path: 'review', name: 'review', component: () => import('../views/review/ComplianceReviewView.vue'), meta: { title: '合规审查', permission: 'review:view' } },
    { path: 'report', name: 'report', component: () => import('../views/report/ReportListView.vue'), meta: { title: '报告管理', permission: 'report:view' } },
    { path: 'report/:id', name: 'reportDetail', component: () => import('../views/report/ReportDetailView.vue'), meta: { title: '报告详情', permission: 'report:view' } },
    { path: 'ocr', name: 'ocr', component: () => import('../views/ocr/OcrView.vue'), meta: { title: 'OCR识别', permission: 'ocr:view' } },
    { path: 'audit', name: 'audit', component: () => import('../views/audit/AuditLogView.vue'), meta: { title: '审计日志', permission: 'audit:view' } },
    { path: 'project/manage', name: 'projectManage', component: () => import('../views/project/ProjectManageView.vue'), meta: { title: '项目设置', permission: 'project:manage' } },
    { path: 'project/members', name: 'projectMembers', component: () => import('../views/project/ProjectMemberView.vue'), meta: { title: '项目成员', permission: 'project:member:manage' } },
    { path: 'system/users', name: 'systemUsers', component: () => import('../views/system/UserManageView.vue'), meta: { title: '用户管理', permission: 'system:user:manage' } },
    { path: 'system/roles', name: 'systemRoles', component: () => import('../views/system/RoleManageView.vue'), meta: { title: '角色管理', permission: 'system:user:manage' } },
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
