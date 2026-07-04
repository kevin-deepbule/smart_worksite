import type { LoginResponse, UserInfo } from '../api/types';

export const mockUser: UserInfo = {
  id: 1,
  username: 'admin',
  realName: '项目管理员',
  roles: ['PROJECT_ADMIN'],
  permissions: ['dashboard:view', 'knowledge:view', 'qa:view', 'review:view', 'report:view', 'ocr:view'],
  defaultProjectId: 1001,
  createdAt: '2026-07-01T09:00:00+08:00',
  updatedAt: '2026-07-04T09:00:00+08:00'
};

export const mockLoginResponse: LoginResponse = {
  accessToken: 'mock-access-token',
  tokenType: 'Bearer',
  expiresIn: 7200,
  user: mockUser
};
