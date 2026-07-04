# 智慧工地前端

Vue 3 + TypeScript + Vite + Pinia + Vue Router + Axios + Element Plus。

## 启动方式

```powershell
cd C:\Users\Dell\Documents\smart_worksite\frontend
npm install
npm run dev
```

默认访问：`http://localhost:5173`。

## 构建方式

```powershell
npm run build
```

构建产物输出到 `dist/`。

## 环境变量

开发环境配置在 `.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_USE_MOCK=true
```

- `VITE_API_BASE_URL`：后端 API 基础地址。
- `VITE_USE_MOCK`：为 `true` 时 API service 返回本地 mock 数据；改为 `false` 后通过 `src/utils/request.ts` 请求后端。

## Mock 模式

mock 数据统一放在 `src/mocks/`，页面通过 `src/api/` service 间接获取数据，不在页面中拼接 URL。

当前 mock 字段尽量贴近接口文档，包含 `id`、`projectId`、`taskId`、`fileId`、`status`、`createdAt`、`updatedAt` 等联调基础字段。

## 目录结构

```text
src/
  api/                 接口 service 和类型
  assets/              全局样式和静态资源
  components/common/   通用上传、表格、状态、JSON、下载等组件
  components/business/ 业务组件预留
  layouts/             主框架布局
  mocks/               本地 mock 数据
  router/              路由和权限守卫
  stores/              Pinia 状态
  utils/               request、requestId 等工具
  views/               登录、首页、知识库、问答、审查、报告、OCR、错误页
```

## History 路由部署说明

前端使用 `createWebHistory()`。生产部署时 Nginx 或静态资源服务器必须把未知前端路由回退到 `index.html`，否则刷新 `/qa`、`/report/10001` 会返回 404。

Nginx 示例：

```nginx
location / {
  try_files $uri $uri/ /index.html;
}
```

## 后续联调说明

- 登录：`POST /api/auth/login`、`GET /api/auth/me`、退出登录接口。
- 项目：`GET /api/projects`，项目切换后刷新当前页面数据。
- 知识库：知识库列表、文档列表、上传文档、触发入库。
- 问答：会话、消息、反馈接口。
- 合规审查：模板列表、提交审查、查询审查结果。
- 报告：列表、详情、重新生成、下载 Word/PDF。
- OCR：提交识别、查询结果、保存人工修订。
- 长任务：报告生成、OCR、知识库入库通过任务详情和阶段日志接口展示进度。
- 下载：统一使用 `downloadFile`，可解析 Blob 中的 JSON 错误和 `Content-Disposition` 文件名。
