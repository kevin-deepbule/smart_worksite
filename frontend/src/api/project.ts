import request from '../utils/request';
import { mockProjects } from '../mocks/project';
import type { ID, PageQuery, PageResult, ProjectCreateForm, ProjectItem, ProjectSettings, ProjectStatistics, ProjectUpdateForm } from './types';
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
  if (useMock) {
    const item = mockProjects.find((project) => String(project.projectId) === String(projectId));
    if (!item) throw new Error(`项目不存在：${projectId}`);
    return mapProject(item);
  }
  const project = await request.get<ProjectResponse>(`/projects/${projectId}`);
  return mapProject(project);
}

export async function createProject(data: ProjectFormPayload | ProjectCreateForm) {
  if (useMock) {
    const seed = mockProjects[0];
    if (!seed) throw new Error('项目 mock 种子数据缺失');
    return mapProject({ ...seed, ...toBackendProject(data), projectId: Date.now(), createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() } as ProjectItem);
  }
  const project = await request.post<ProjectResponse>('/projects', toBackendProject(data));
  return mapProject(project);
}

export async function updateProject(projectId: ID, data: ProjectFormPayload | ProjectUpdateForm) {
  if (useMock) {
    const item = mockProjects.find((project) => String(project.projectId) === String(projectId));
    if (!item) throw new Error(`项目不存在：${projectId}`);
    return mapProject({ ...item, ...toBackendProject(data) } as ProjectItem);
  }
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

export function enableProject(projectId: ID) {
  if (useMock) return Promise.resolve();
  return request.post<void>(`/projects/${projectId}/enable`);
}

export function disableProject(projectId: ID) {
  if (useMock) return Promise.resolve();
  return request.post<void>(`/projects/${projectId}/disable`);
}

export function archiveProject(projectId: ID) {
  if (useMock) return Promise.resolve();
  return request.post<void>(`/projects/${projectId}/archive`);
}

export function fetchProjectSettings(projectId: ID) {
  if (useMock) {
    return Promise.resolve({
      projectId,
      dataRetentionDays: 365,
      uploadMaxSizeMb: 100,
      allowedFileTypes: ['docx', 'pdf', 'jpg', 'png'],
      internetPolicyCrawlerEnabled: false,
      defaultQaRouteMode: 'AUTO',
      defaultOcrLanguage: 'zh-CN',
      defaultReportExportFormat: 'WORD'
    } satisfies ProjectSettings);
  }
  return request.get<ProjectSettings>(`/projects/${projectId}/settings`);
}

export function updateProjectSettings(projectId: ID, data: ProjectSettings) {
  if (useMock) return Promise.resolve({ ...data, projectId } satisfies ProjectSettings);
  return request.put<ProjectSettings>(`/projects/${projectId}/settings`, data);
}

export function fetchProjectStatistics(projectId: ID) {
  if (useMock) {
    return Promise.resolve({
      projectId,
      memberCount: 3,
      knowledgeBaseCount: 2,
      reportCount: 1,
      dataSourceCount: 1,
      qaCount: 2,
      reviewCount: 1,
      ocrCount: 1,
      fileStorageBytes: 2048000
    } satisfies ProjectStatistics);
  }
  return request.get<ProjectStatistics>(`/projects/${projectId}/statistics`);
}
