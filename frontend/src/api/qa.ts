import request from '../utils/request';
import { mockQaMessages, mockQaSessions } from '../mocks/qa';
import type { ID, QaMessage, QaSession } from './types';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export async function createQaSession(data: { projectId: ID; title?: string }) {
  if (useMock) return { ...mockQaSessions[0], ...data, id: Date.now(), sessionId: Date.now() } satisfies QaSession;
  return request.post<QaSession>('/qa/sessions', data);
}

export async function fetchQaSessions(projectId: ID) {
  if (useMock) return mockQaSessions.filter((item) => String(item.projectId) === String(projectId));
  return request.get<QaSession[]>('/qa/sessions', { params: { projectId } });
}

export async function fetchQaMessages(sessionId: ID) {
  if (useMock) return mockQaMessages.filter((item) => String(item.sessionId) === String(sessionId));
  return request.get<QaMessage[]>(`/qa/sessions/${sessionId}/messages`);
}

export async function sendQuestion(sessionId: ID, data: { projectId: ID; question: string; routeMode?: string; dataSourceIds?: ID[]; knowledgeBaseIds?: ID[] }) {
  if (useMock) {
    const now = new Date().toISOString();
    return {
      id: Date.now(), messageId: Date.now(), sessionId, projectId: data.projectId, taskId: 9301, fileId: 0, role: 'assistant', question: data.question,
      answer: '临边防护应设置防护栏杆、挡脚板和安全网，洞口需采用盖板或围栏并设置警示标识。',
      content: '临边防护应设置防护栏杆、挡脚板和安全网，洞口需采用盖板或围栏并设置警示标识。',
      routeMode: 'MIXED', status: 'SUCCESS', createdAt: now, updatedAt: now,
      references: [
        { title: 'JGJ 80-2016 高处作业安全技术规范', sourceType: 'KNOWLEDGE', page: '第12页', score: 0.92, documentId: 3001 },
        { title: '项目安全检查制度', sourceType: 'KNOWLEDGE', page: '第4章', score: 0.86, documentId: 3002 }
      ]
    } satisfies QaMessage;
  }
  return request.post<QaMessage>(`/qa/sessions/${sessionId}/messages`, data);
}

export async function submitFeedback(messageId: ID, useful: boolean) {
  if (useMock) return { messageId, useful };
  return request.post(`/qa/messages/${messageId}/feedback`, { useful });
}
