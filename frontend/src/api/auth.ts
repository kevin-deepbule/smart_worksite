import request from '../utils/request';
import { mockLoginResponse, mockUser } from '../mocks/auth';
import type { LoginRequest, LoginResponse, UserInfo } from './types';

const useMock = import.meta.env.VITE_USE_MOCK === 'true';

export async function login(data: LoginRequest) {
  if (useMock) return { ...mockLoginResponse, accessToken: `mock-token-${Date.now()}`, user: { ...mockUser, username: data.username } } satisfies LoginResponse;
  return request.post<LoginResponse>('/auth/login', data);
}

export async function fetchCurrentUser() {
  if (useMock) return mockUser;
  return request.get<UserInfo>('/auth/me');
}

export async function logout() {
  if (useMock) return null;
  return request.post<null>('/auth/logout');
}
