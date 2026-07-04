import type { OcrRecord } from '../api/types';

export const mockOcrRecord: OcrRecord = {
  id: 11001,
  recordId: 11001,
  projectId: 1001,
  taskId: 9601,
  fileId: 4401,
  ocrType: 'CONTRACT',
  status: 'PROCESSING',
  progress: 88,
  fields: [
    { fieldName: '车牌号', fieldValue: '浙A12345', confidence: 0.98, location: '第1页' },
    { fieldName: '施工单位', fieldValue: '某某建设集团', confidence: 0.91, location: '第1页' },
    { fieldName: '金额', fieldValue: '12800.00', confidence: 0.84, location: '第2页' }
  ],
  createdAt: '2026-07-04T10:00:00+08:00',
  updatedAt: '2026-07-04T10:08:00+08:00'
};
