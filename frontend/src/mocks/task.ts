import type { TaskDetail, TaskStageLog } from '../api/types';

export const mockTaskStages: TaskStageLog[] = [
  { id: 1, projectId: 1001, taskId: 9502, fileId: 4302, stageName: '数据收集', status: 'SUCCESS', message: '项目数据和知识库引用收集完成', createdAt: '2026-07-04T09:00:00+08:00', updatedAt: '2026-07-04T09:00:00+08:00' },
  { id: 2, projectId: 1001, taskId: 9502, fileId: 4302, stageName: 'AI生成', status: 'PROCESSING', message: '正在生成报告章节内容', createdAt: '2026-07-04T09:05:00+08:00', updatedAt: '2026-07-04T09:15:00+08:00' }
];

export const mockTaskDetail: TaskDetail = {
  id: 9502,
  projectId: 1001,
  taskId: 9502,
  fileId: 4302,
  taskType: 'REPORT_GENERATE',
  status: 'PROCESSING',
  progress: 64,
  stageLogs: mockTaskStages,
  createdAt: '2026-07-04T09:00:00+08:00',
  updatedAt: '2026-07-04T09:15:00+08:00'
};
