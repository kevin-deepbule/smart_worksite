import request from '../utils/request';
import { mockOcrRecord } from '../mocks/ocr';
import type { ID, OcrField, OcrRecord } from './types';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export async function submitOcrRecord(data: { projectId: ID; ocrType: string; file: File; customFields?: string }) {
  if (useMock) return { recordId: mockOcrRecord.recordId, taskId: mockOcrRecord.taskId, status: mockOcrRecord.status };
  const form = new FormData();
  form.append('projectId', String(data.projectId));
  form.append('ocrType', data.ocrType);
  form.append('file', data.file);
  if (data.customFields) form.append('customFields', data.customFields);
  return request.post<{ recordId: ID; taskId: ID; status: string }>('/ocr/records', form);
}

export async function fetchOcrRecord(recordId: ID) {
  if (useMock) return { ...mockOcrRecord, recordId } satisfies OcrRecord;
  return request.get<OcrRecord>(`/ocr/records/${recordId}`);
}

export async function updateOcrFields(recordId: ID, fields: OcrField[]) {
  if (useMock) return { recordId, fields, status: 'SUCCESS' };
  return request.put(`/ocr/records/${recordId}/fields`, { fields });
}
