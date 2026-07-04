import request from '../utils/request';
import { mockReviewRecord, mockReviewTemplates } from '../mocks/review';
import type { ID, ReviewRecord, ReviewTemplate } from './types';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export async function uploadReviewTemplate(data: { projectId: ID; templateName: string; templateType: string; file: File }) {
  if (useMock) return { ...mockReviewTemplates[0], ...data, fileId: 4101, id: Date.now(), templateId: Date.now() } satisfies ReviewTemplate;
  const form = new FormData();
  form.append('projectId', String(data.projectId));
  form.append('templateName', data.templateName);
  form.append('templateType', data.templateType);
  form.append('file', data.file);
  return request.post<ReviewTemplate>('/review/templates', form);
}

export async function fetchReviewTemplates(projectId?: ID) {
  if (useMock) return projectId ? mockReviewTemplates.filter((item) => String(item.projectId) === String(projectId)) : mockReviewTemplates;
  return request.get<ReviewTemplate[]>('/review/templates', { params: { projectId } });
}

export async function submitReviewRecord(data: { projectId: ID; templateId: ID; file: File }) {
  if (useMock) return { recordId: mockReviewRecord.recordId, taskId: mockReviewRecord.taskId, status: mockReviewRecord.status };
  const form = new FormData();
  form.append('projectId', String(data.projectId));
  form.append('templateId', String(data.templateId));
  form.append('file', data.file);
  return request.post<{ recordId: ID; taskId: ID; status: string }>('/review/records', form);
}

export async function fetchReviewRecord(recordId: ID) {
  if (useMock) return { ...mockReviewRecord, recordId } satisfies ReviewRecord;
  return request.get<ReviewRecord>(`/review/records/${recordId}`);
}
