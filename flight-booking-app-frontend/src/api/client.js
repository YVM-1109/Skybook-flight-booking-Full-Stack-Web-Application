import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const client = axios.create({
  baseURL: API_BASE_URL,
});

// Access token lives in memory + localStorage (survives refresh).
// Backend issues a 15-minute access token and a 7-day refresh token
// (see AuthController / JwtTokenProvider) — there's no HttpOnly cookie
// wired up on the backend for refresh, so we store both client-side.
export function getAccessToken() {
  return localStorage.getItem('accessToken');
}
export function getRefreshToken() {
  return localStorage.getItem('refreshToken');
}
export function setTokens({ accessToken, refreshToken }) {
  if (accessToken) localStorage.setItem('accessToken', accessToken);
  if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
}
export function clearTokens() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

client.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let refreshPromise = null;

client.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const isAuthRoute = original?.url?.includes('/auth/');

    if (status === 401 && !original._retry && !isAuthRoute && getRefreshToken()) {
      original._retry = true;
      try {
        if (!refreshPromise) {
          refreshPromise = axios
            .post(`${API_BASE_URL}/auth/refresh`, { refreshToken: getRefreshToken() })
            .then((res) => res.data.data)
            .finally(() => { refreshPromise = null; });
        }
        const data = await refreshPromise;
        setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
        original.headers.Authorization = `Bearer ${data.accessToken}`;
        return client(original);
      } catch (refreshErr) {
        clearTokens();
        window.location.href = '/login';
        return Promise.reject(refreshErr);
      }
    }

    return Promise.reject(error);
  }
);

// Normalizes the backend's ApiResponse<T> envelope { success, message, data }
// and surfaces a readable message on failure.
export function unwrap(promise) {
  return promise
    .then((res) => res.data.data)
    .catch((err) => {
      const msg =
        err.response?.data?.message ||
        (err.response?.status === 0 ? 'Cannot reach the server' : err.message) ||
        'Something went wrong';
      throw new Error(msg);
    });
}
