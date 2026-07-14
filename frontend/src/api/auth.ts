import request from '../utils/request';
import { mockLoginResponse, mockUser } from '../mocks/auth';
import type { LoginRequest, LoginResponse, UserInfo } from './types';
import { useModuleMock } from './mock';

const useMock = useModuleMock('VITE_USE_AUTH_MOCK', false);

export async function login(data: LoginRequest) {
  if (useMock) return { ...mockLoginResponse, accessToken: `mock-token-${Date.now()}`, user: { ...mockUser, username: data.username } } satisfies LoginResponse;
  return request.post<LoginResponse>('/auth/login', data);
}

export async function fetchCurrentUser() {
  if (useMock) {
    const raw = localStorage.getItem('smart_worksite_user');
    if (!raw) return mockUser;
    try {
      return JSON.parse(raw) as UserInfo;
    } catch (error) {
      console.error('Stored mock user state is corrupted.', error);
      throw new Error('本地 mock 用户缓存解析失败');
    }
  }
  return request.get<UserInfo>('/auth/me');
}

export async function logout() {
  if (useMock) return null;
  return request.post<null>('/auth/logout');
}

export async function changeCurrentPassword(data: { oldPassword: string; newPassword: string }) {
  if (useMock) return null;
  return request.put<null>('/auth/me/password', data);
}
