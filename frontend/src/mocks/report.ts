import type { ReportItem } from '../api/types';

export const mockReports: ReportItem[] = [
  { id: 10001, reportId: 10001, projectId: 1001, taskId: 9501, fileId: 4301, reportName: '2026年6月安全月报', reportType: 'SAFETY_MONTHLY', templateId: 9001, version: 'v3', status: 'SUCCESS', progress: 100, createdBy: '资料员', createdAt: '2026-07-03T09:00:00+08:00', updatedAt: '2026-07-03T18:00:00+08:00' },
  { id: 10002, reportId: 10002, projectId: 1001, taskId: 9502, fileId: 4302, reportName: '质量巡检周报', reportType: 'QUALITY_WEEKLY', templateId: 9002, version: 'v1', status: 'PROCESSING', progress: 64, createdBy: '质量员', createdAt: '2026-07-04T08:30:00+08:00', updatedAt: '2026-07-04T09:20:00+08:00' }
];
