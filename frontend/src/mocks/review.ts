import type { ReviewRecord, ReviewTemplate } from '../api/types';

export const mockReviewTemplates: ReviewTemplate[] = [
  { id: 7001, templateId: 7001, projectId: 1001, taskId: 0, fileId: 4101, templateName: '安全专项方案', templateType: 'SAFETY_PLAN', status: 'ACTIVE', createdAt: '2026-07-01T09:00:00+08:00', updatedAt: '2026-07-04T09:00:00+08:00' },
  { id: 7002, templateId: 7002, projectId: 1001, taskId: 0, fileId: 4102, templateName: '质量验收资料', templateType: 'QUALITY_ACCEPTANCE', status: 'ACTIVE', createdAt: '2026-07-01T09:10:00+08:00', updatedAt: '2026-07-04T09:10:00+08:00' }
];

export const mockReviewRecord: ReviewRecord = {
  id: 8001,
  recordId: 8001,
  projectId: 1001,
  taskId: 9401,
  fileId: 4201,
  templateId: 7001,
  status: 'PROCESSING',
  progress: 72,
  issues: [
    { issueId: 'ISSUE-001', severity: 'HIGH', location: '第3章 3.2', ruleName: '安全措施完整性要求', description: '缺少深基坑监测频次和报警阈值。', suggestion: '补充深基坑监测频次、报警阈值和责任人。' },
    { issueId: 'ISSUE-002', severity: 'MEDIUM', location: '附件A', ruleName: '签章完整性要求', description: '缺少项目负责人签字页。', suggestion: '补齐项目负责人签字和日期。' }
  ],
  createdAt: '2026-07-04T10:00:00+08:00',
  updatedAt: '2026-07-04T10:20:00+08:00'
};
