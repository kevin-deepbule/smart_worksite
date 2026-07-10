import request from '../utils/request';
import { mockProjects } from '../mocks/project';
import type { ID, PageQuery, PageResult, ProjectCreateForm, ProjectItem, ProjectMember, ProjectUpdateForm } from './types';
import { useModuleMock } from './mock';

const useMock = useModuleMock('VITE_USE_PROJECT_MOCK', false);

interface ProjectResponse {
  projectId: number;
  projectName: string;
  projectCode: string;
  location?: string;
  status: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

function mapProject(item: ProjectResponse | ProjectItem): ProjectItem {
  const projectName = ('projectName' in item && item.projectName ? item.projectName : (item as ProjectItem).name) || '';
  const projectCode = ('projectCode' in item && item.projectCode ? item.projectCode : (item as ProjectItem).code) || '';
  const location = ('location' in item && item.location ? item.location : (item as ProjectItem).address) || '';
  return {
    id: (item as ProjectItem).id || item.projectId,
    projectId: item.projectId,
    name: projectName,
    code: projectCode,
    address: location || '',
    projectName,
    projectCode,
    location: location || '',
    status: item.status,
    description: item.description || '',
    createdAt: item.createdAt || '',
    updatedAt: item.updatedAt || item.createdAt || ''
  };
}

function mapProjectPage(page: PageResult<ProjectResponse | ProjectItem>): PageResult<ProjectItem> {
  return { ...page, records: page.records.map(mapProject) };
}

export interface ProjectFormPayload {
  name: string;
  code: string;
  address?: string;
  description?: string;
}

function toBackendProject(data: ProjectFormPayload | ProjectCreateForm | ProjectUpdateForm) {
  if ('projectName' in data || 'projectCode' in data) return data;
  return {
    projectName: data.name.trim(),
    projectCode: data.code.trim(),
    location: data.address?.trim() || undefined,
    description: data.description?.trim() || undefined
  };
}

export async function fetchProjects(params: PageQuery = {}) {
  if (useMock) {
    return { pageNo: params.pageNo || 1, pageSize: params.pageSize || 20, total: mockProjects.length, records: mockProjects.map(mapProject) } satisfies PageResult<ProjectItem>;
  }
  const page = await request.get<PageResult<ProjectResponse>>('/projects', { params });
  return mapProjectPage(page);
}

export async function fetchProjectDetail(projectId: ID) {
  if (useMock) return mapProject(mockProjects.find((item) => String(item.projectId) === String(projectId)) || mockProjects[0]);
  const project = await request.get<ProjectResponse>(`/projects/${projectId}`);
  return mapProject(project);
}

export async function createProject(data: ProjectFormPayload | ProjectCreateForm) {
  if (useMock) return mapProject({ ...mockProjects[0], ...toBackendProject(data), projectId: Date.now(), createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() } as ProjectItem);
  const project = await request.post<ProjectResponse>('/projects', toBackendProject(data));
  return mapProject(project);
}

export async function updateProject(projectId: ID, data: ProjectFormPayload | ProjectUpdateForm) {
  if (useMock) return mapProject({ ...(mockProjects.find((item) => String(item.projectId) === String(projectId)) || mockProjects[0]), ...toBackendProject(data) } as ProjectItem);
  const project = await request.put<ProjectResponse>(`/projects/${projectId}`, toBackendProject(data));
  return mapProject(project);
}

export function deleteProject(projectId: ID) {
  if (useMock) return Promise.resolve();
  return request.delete<void>(`/projects/${projectId}`);
}

export function updateProjectStatus(projectId: ID, status: string) {
  if (useMock) return Promise.resolve();
  return request.put<void>(`/projects/${projectId}/status`, { status });
}

const mockMembers: ProjectMember[] = [
  { id: 1, userId: 1, projectId: 1001, realName: '项目管理员', roleName: '项目管理员', permissions: ['*'], status: 'ACTIVE', createdAt: '2026-07-01T09:00:00+08:00' },
  { id: 2, userId: 2, projectId: 1001, realName: '审核员', roleName: '审核员', permissions: ['review:view', 'ocr:view'], status: 'ACTIVE', createdAt: '2026-07-02T09:00:00+08:00' }
];

export async function fetchProjectMembers(projectId: ID) {
  if (useMock) return mockMembers.filter((item) => String(item.projectId) === String(projectId));
  void projectId;
  return [] as ProjectMember[];
}

export async function addProjectMember(projectId: ID, data: { userId: ID; roleName: string }) {
  if (useMock) return { id: Date.now(), projectId, userId: data.userId, realName: `用户${data.userId}`, roleName: data.roleName, permissions: [], status: 'ACTIVE', createdAt: new Date().toISOString() } satisfies ProjectMember;
  throw new Error('项目成员新增接口暂未接入');
}

export async function fetchProjectPermissions(projectId: ID) {
  if (useMock) return ['dashboard:view', 'project:view', 'file:view', 'template:view', 'knowledge:view', 'qa:view', 'review:view', 'report:view', 'ocr:view', 'datasource:view', 'task:view', 'audit:view'];
  void projectId;
  return [] as string[];
}
