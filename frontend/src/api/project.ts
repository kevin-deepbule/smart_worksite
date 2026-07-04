import request from '../utils/request';
import { mockProjects } from '../mocks/project';
import type { PageQuery, PageResult, ProjectItem } from './types';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export async function fetchProjects(params: PageQuery = {}) {
  if (useMock) return { pageNo: params.pageNo || 1, pageSize: params.pageSize || 20, total: mockProjects.length, records: mockProjects } satisfies PageResult<ProjectItem>;
  return request.get<PageResult<ProjectItem>>('/projects', { params });
}

export async function fetchProjectDetail(projectId: string | number) {
  if (useMock) return mockProjects.find((item) => String(item.projectId) === String(projectId)) || mockProjects[0];
  return request.get<ProjectItem>(`/projects/${projectId}`);
}
