import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';
import { router } from '../router';
import { useUserStore } from '../stores/user';
import { createRequestId } from './id';

export interface ApiResponse<T = unknown> {
  code: number;
  message?: string;
  data?: T | null;
  requestId?: string;
  timestamp?: string;
}

export interface DownloadOptions extends AxiosRequestConfig {
  filename?: string;
}

let authRedirecting = false;

type RequestInstance = Omit<AxiosInstance, 'get' | 'post' | 'put' | 'delete' | 'request'> & {
  request<T = unknown, R = T>(config: AxiosRequestConfig): Promise<R>;
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>;
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>;
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>;
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>;
};

export const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
}) as RequestInstance;

function isApiResponse(value: unknown): value is ApiResponse {
  return Boolean(value && typeof value === 'object' && 'code' in value);
}

function getHeader(headers: AxiosResponse['headers'], key: string) {
  const lowerKey = key.toLowerCase();
  const value = headers[key] ?? headers[lowerKey];
  return Array.isArray(value) ? value[0] : value;
}

function parseFilename(contentDisposition?: string, fallback = 'download') {
  if (!contentDisposition) return fallback;
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) return decodeURIComponent(utf8Match[1]);
  const normalMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  return normalMatch?.[1] ? decodeURIComponent(normalMatch[1]) : fallback;
}

async function parseBlobJson(blob: Blob) {
  const text = await blob.text();
  try {
    return JSON.parse(text) as ApiResponse;
  } catch {
    return null;
  }
}

async function handleUnauthorized() {
  if (authRedirecting) return;
  authRedirecting = true;
  const userStore = useUserStore();
  userStore.clearAuthState();
  await router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } });
  window.setTimeout(() => { authRedirecting = false; }, 300);
}

request.interceptors.request.use((config) => {
  const userStore = useUserStore();
  config.headers = config.headers || {};
  config.headers['X-Request-Id'] = createRequestId();
  if (userStore.token) config.headers.Authorization = `Bearer ${userStore.token}`;
  return config;
});

request.interceptors.response.use(
  async <T>(response: AxiosResponse<ApiResponse<T> | T | Blob>) => {
    if (response.config.responseType === 'blob' && response.data instanceof Blob) {
      const contentType = getHeader(response.headers, 'content-type') || response.data.type;
      if (String(contentType).includes('application/json')) {
        const apiError = await parseBlobJson(response.data);
        if (apiError && apiError.code !== 0) {
          if (apiError.code === 40100) await handleUnauthorized();
          else if (apiError.code === 40300) await router.replace('/403');
          else ElMessage.error(apiError.message || '下载失败');
          return Promise.reject(new Error(apiError.message || '下载失败'));
        }
      }
      return response as unknown as T;
    }

    const body = response.data;
    if (isApiResponse(body)) {
      if (body.code === 0) return (body.data ?? null) as T;
      if (body.code === 40100) {
        await handleUnauthorized();
        return Promise.reject(new Error(body.message || '未登录或登录已过期'));
      }
      if (body.code === 40300) {
        await router.replace('/403');
        return Promise.reject(new Error(body.message || '无权限访问'));
      }
      ElMessage.error(body.message || '请求失败');
      return Promise.reject(new Error(body.message || '请求失败'));
    }
    return body as T;
  },
  async (error: AxiosError<ApiResponse | Blob>) => {
    const status = error.response?.status;
    let message = '网络异常';
    if (error.response?.data instanceof Blob) {
      const apiError = await parseBlobJson(error.response.data);
      message = apiError?.message || error.message || message;
    } else {
      message = error.response?.data?.message || error.message || message;
    }
    if (status === 401) await handleUnauthorized();
    else if (status === 403) await router.replace('/403');
    else ElMessage.error(message);
    return Promise.reject(error);
  }
);

export async function downloadFile(url: string, options: DownloadOptions = {}) {
  if (options.data && !url) {
    const blob = new Blob([String(options.data)], { type: 'text/plain;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = options.filename || 'download.txt';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
    return;
  }
    const response = await request.request<Blob, AxiosResponse<Blob>>({
    ...options,
    url,
    method: options.method || 'GET',
    responseType: 'blob',
    transformResponse: [(data) => data]
  });
  const blob = response.data;
  const headerFilename = parseFilename(getHeader(response.headers, 'content-disposition'), options.filename || 'download');
  const filename = options.filename || headerFilename;
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(link.href);
}

export default request;




