import type { KnowledgeBase, KnowledgeDocument } from '../api/types';

export const mockKnowledgeBases: KnowledgeBase[] = [
  { id: 2001, projectId: 1001, taskId: 9101, fileId: 0, name: '项目安全规范库', description: '安全生产制度、专项方案和标准规范', status: 'ACTIVE', documentCount: 126, createdAt: '2026-07-01T10:00:00+08:00', updatedAt: '2026-07-04T10:00:00+08:00' },
  { id: 2002, projectId: 1001, taskId: 9102, fileId: 0, name: '施工质量验收库', description: '质量验收规范和项目验收记录', status: 'ACTIVE', documentCount: 88, createdAt: '2026-07-01T10:10:00+08:00', updatedAt: '2026-07-04T10:10:00+08:00' }
];

export const mockKnowledgeDocuments: KnowledgeDocument[] = [
  { id: 3001, documentId: 3001, projectId: 1001, knowledgeBaseId: 2001, taskId: 9201, fileId: 4001, fileName: '临边防护检查标准.pdf', parseStatus: 'SUCCESS', indexStatus: 'SUCCESS', status: 'SUCCESS', failReason: '', createdAt: '2026-07-02T09:00:00+08:00', updatedAt: '2026-07-04T11:00:00+08:00' },
  { id: 3002, documentId: 3002, projectId: 1001, knowledgeBaseId: 2001, taskId: 9202, fileId: 4002, fileName: '脚手架专项方案.docx', parseStatus: 'PROCESSING', indexStatus: 'PENDING', status: 'PROCESSING', failReason: '等待切片入库', createdAt: '2026-07-03T09:00:00+08:00', updatedAt: '2026-07-04T11:05:00+08:00' }
];
