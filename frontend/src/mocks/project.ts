import type { ProjectItem } from '../api/types';

export const mockProjects: ProjectItem[] = [
  { id: 1001, projectId: 1001, name: '城北智慧工地一期', code: 'CB-2026-01', status: 'ACTIVE', address: '杭州市城北片区', taskId: 9001, fileId: 0, createdAt: '2026-07-01T09:00:00+08:00', updatedAt: '2026-07-04T09:00:00+08:00' },
  { id: 1002, projectId: 1002, name: '轨交站点综合体', code: 'GJ-ZD-02', status: 'ACTIVE', address: '地铁二号线站点', taskId: 9002, fileId: 0, createdAt: '2026-07-01T09:00:00+08:00', updatedAt: '2026-07-04T09:10:00+08:00' },
  { id: 1003, projectId: 1003, name: '产业园改造工程', code: 'CY-GZ-03', status: 'DISABLED', address: '高新区产业园', taskId: 9003, fileId: 0, createdAt: '2026-07-01T09:00:00+08:00', updatedAt: '2026-07-04T09:20:00+08:00' }
];
