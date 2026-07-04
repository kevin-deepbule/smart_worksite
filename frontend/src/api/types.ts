export type ID = string | number;
export type Status = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'SUCCESS' | 'FAILED' | 'ACTIVE' | 'DISABLED' | 'ARCHIVED';

export interface PageQuery {
  pageNo?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  projectId?: ID;
}

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface UserInfo {
  id: ID;
  username: string;
  realName: string;
  roles: string[];
  permissions: string[];
  defaultProjectId?: ID;
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: UserInfo;
}

export interface ProjectItem {
  id: ID;
  projectId: ID;
  name: string;
  code: string;
  status: Status;
  address: string;
  taskId?: ID;
  fileId?: ID;
  createdAt: string;
  updatedAt: string;
}

export interface FileObject {
  id: ID;
  fileId: ID;
  projectId: ID;
  taskId?: ID;
  originalName: string;
  fileType: string;
  size: number;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeBase {
  id: ID;
  projectId: ID;
  taskId?: ID;
  fileId?: ID;
  name: string;
  description: string;
  status: Status;
  documentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeDocument {
  id: ID;
  documentId: ID;
  projectId: ID;
  knowledgeBaseId: ID;
  taskId: ID;
  fileId: ID;
  fileName: string;
  parseStatus: Status;
  indexStatus: Status;
  status: Status;
  failReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface QaSession {
  id: ID;
  sessionId: ID;
  projectId: ID;
  taskId?: ID;
  fileId?: ID;
  title: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

export interface QaReference {
  title: string;
  sourceType: 'KNOWLEDGE' | 'DATABASE' | 'POLICY';
  page?: string;
  score: number;
  documentId?: ID;
}

export interface QaMessage {
  id: ID;
  messageId: ID;
  sessionId: ID;
  projectId: ID;
  taskId?: ID;
  fileId?: ID;
  role: 'user' | 'assistant';
  question?: string;
  answer?: string;
  content: string;
  routeMode?: 'AUTO' | 'MODEL' | 'KNOWLEDGE' | 'DATABASE' | 'MIXED';
  references?: QaReference[];
  status: Status;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewTemplate {
  id: ID;
  templateId: ID;
  projectId: ID;
  taskId?: ID;
  fileId: ID;
  templateName: string;
  templateType: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

export interface ReviewIssue {
  issueId: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  location: string;
  ruleName: string;
  description: string;
  suggestion: string;
}

export interface ReviewRecord {
  id: ID;
  recordId: ID;
  projectId: ID;
  taskId: ID;
  fileId: ID;
  templateId: ID;
  status: Status;
  progress: number;
  issues: ReviewIssue[];
  createdAt: string;
  updatedAt: string;
}

export interface ReportItem {
  id: ID;
  reportId: ID;
  projectId: ID;
  taskId: ID;
  fileId?: ID;
  reportName: string;
  reportType: string;
  templateId: ID;
  version: string;
  status: Status;
  progress: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface OcrField {
  fieldName: string;
  fieldValue: string;
  confidence: number;
  location: string;
}

export interface OcrRecord {
  id: ID;
  recordId: ID;
  projectId: ID;
  taskId: ID;
  fileId: ID;
  ocrType: 'ID_CARD' | 'LICENSE_PLATE' | 'INVOICE' | 'CONTRACT' | 'CUSTOM';
  status: Status;
  progress: number;
  fields: OcrField[];
  createdAt: string;
  updatedAt: string;
}

export interface TaskStageLog {
  id: ID;
  projectId: ID;
  taskId: ID;
  fileId?: ID;
  stageName: string;
  status: Status;
  message: string;
  createdAt: string;
  updatedAt: string;
}

export interface TaskDetail {
  id: ID;
  projectId: ID;
  taskId: ID;
  fileId?: ID;
  taskType: string;
  status: Status;
  progress: number;
  stageLogs: TaskStageLog[];
  createdAt: string;
  updatedAt: string;
}
