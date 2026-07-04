import request, { downloadFile } from '../utils/request';
import { mockFiles } from '../mocks/file';
import type { FileObject, ID, PageQuery, PageResult } from './types';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export async function uploadFile(projectId: ID, file: File, businessType = 'COMMON') {
  if (useMock) return { ...mockFiles[0], projectId, originalName: file.name } satisfies FileObject;
  const form = new FormData();
  form.append('projectId', String(projectId));
  form.append('businessType', businessType);
  form.append('file', file);
  return request.post<FileObject>('/files', form);
}

export async function fetchFiles(params: PageQuery = {}) {
  if (useMock) return { pageNo: params.pageNo || 1, pageSize: params.pageSize || 20, total: mockFiles.length, records: mockFiles } satisfies PageResult<FileObject>;
  return request.get<PageResult<FileObject>>('/files', { params });
}

export function downloadByFileId(fileId: ID, filename?: string) {
  if (useMock) return downloadFile('', { filename, data: 'mock file content' });
  return downloadFile(`/files/${fileId}/download`, { filename });
}
