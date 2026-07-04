import request from '../utils/request';
import { mockTaskDetail, mockTaskStages } from '../mocks/task';
import type { ID, TaskDetail, TaskStageLog } from './types';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export async function fetchTaskDetail(taskId: ID) {
  if (useMock) return { ...mockTaskDetail, taskId } satisfies TaskDetail;
  return request.get<TaskDetail>(`/tasks/${taskId}`);
}

export async function fetchTaskStages(taskId: ID) {
  if (useMock) return mockTaskStages.map((item) => ({ ...item, taskId })) satisfies TaskStageLog[];
  return request.get<TaskStageLog[]>(`/tasks/${taskId}/stages`);
}

export async function retryTask(taskId: ID) {
  if (useMock) return { taskId, status: 'PROCESSING' };
  return request.post(`/tasks/${taskId}/retry`);
}

export async function cancelTask(taskId: ID) {
  if (useMock) return { taskId, status: 'CANCELLED' };
  return request.post(`/tasks/${taskId}/cancel`);
}
