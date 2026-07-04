import type { QaMessage, QaSession } from '../api/types';

export const mockQaSessions: QaSession[] = [
  { id: 5001, sessionId: 5001, projectId: 1001, taskId: 0, fileId: 0, title: '安全规范问答', status: 'ACTIVE', createdAt: '2026-07-04T09:00:00+08:00', updatedAt: '2026-07-04T09:30:00+08:00' },
  { id: 5002, sessionId: 5002, projectId: 1001, taskId: 0, fileId: 0, title: '质量验收咨询', status: 'ACTIVE', createdAt: '2026-07-04T10:00:00+08:00', updatedAt: '2026-07-04T10:30:00+08:00' }
];

export const mockQaMessages: QaMessage[] = [
  { id: 6001, messageId: 6001, sessionId: 5001, projectId: 1001, taskId: 9301, fileId: 0, role: 'assistant', content: '请围绕当前项目知识库提问，我会返回来源引用和可追溯结果。', answer: '请围绕当前项目知识库提问，我会返回来源引用和可追溯结果。', routeMode: 'MIXED', references: [], status: 'SUCCESS', createdAt: '2026-07-04T09:01:00+08:00', updatedAt: '2026-07-04T09:01:00+08:00' }
];
