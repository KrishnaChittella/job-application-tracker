import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE + '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT from localStorage to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// On 401, clear token and notify auth context so user is logged out and redirected
api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.dispatchEvent(new CustomEvent('auth:logout'));
    }
    return Promise.reject(err);
  }
);

// Auth
export const register = (data) => api.post('/auth/register', data);
export const login = (data) => api.post('/auth/login', data);

// Applications
export const getApplications = (params) => api.get('/applications', { params });
export const getApplicationById = (id) => api.get(`/applications/${id}`);
export const createApplication = (data) => api.post('/applications', data);
export const updateApplication = (id, data) => api.put(`/applications/${id}`, data);
export const deleteApplication = (id) => api.delete(`/applications/${id}`);

// Dashboard
export const getDashboard = () => api.get('/applications/dashboard');

// Outlook
export const getOutlookAuthorizeUrl = (token) => api.get('/outlook/authorize', { params: { token } });
export const outlookCallback = (code, state) => api.post('/outlook/callback', { code, state });
export const getOutlookStatus = () => api.get('/outlook/status');
export const outlookSync = () => api.post('/outlook/sync');
