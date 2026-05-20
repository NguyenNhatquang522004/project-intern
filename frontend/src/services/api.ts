import axios from 'axios';
import { UserTask, BaseResponse, FilterSummary } from '@/types/task';
import { getSession } from 'next-auth/react';

const API_BASE_URL = 'http://localhost:8081/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json', 
  },
});

// Interceptor tự động lấy Access Token từ NextAuth Session và đính kèm vào Header của mọi request
apiClient.interceptors.request.use(async (config) => {
  if (typeof window !== 'undefined') {
    const session = await getSession();
    if (session && session.accessToken) {
      config.headers.Authorization = `Bearer ${session.accessToken}`;
    }
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});
apiClient.interceptors.response.use(
  (response) => {
    if (response.data && response.data.code === '401') {
      if (typeof window !== 'undefined') {
        import('next-auth/react').then(({ signOut }) => {
          signOut({ callbackUrl: '/login' });
        });
      }
    }
    return response;
  },
  async (error) => {
    if (error.response && error.response.status === 401) {
      if (typeof window !== 'undefined') {
        const { signOut } = await import('next-auth/react');
        signOut({ callbackUrl: '/login' });
      }
    }
    return Promise.reject(error);
  }
);

export const apiService = {
  // 1. Lấy thống kê số lượng task theo 4 bộ lọc
  getFiltersSummary: async (): Promise<FilterSummary> => {
    const response = await apiClient.get<BaseResponse<FilterSummary>>('/tasklist/filters-summary');
    if (response.data.code !== '200') {
      throw new Error(response.data.message || 'Lỗi lấy thông tin thống kê');
    }
    return response.data.data || { allOpen: 0, assignedToMe: 0, unassigned: 0, completed: 0 };
  },

  // 2. Lấy danh sách task theo bộ lọc
  getTasks: async (filter: string): Promise<UserTask[]> => {
    const response = await apiClient.get<BaseResponse<UserTask[]>>(`/tasklist/tasks?filter=${filter}`);
    if (response.data.code !== '200') {
      throw new Error(response.data.message || 'Lỗi lấy danh sách công việc');
    }
    return response.data.data || [];
  },

  // 3. Nhận việc (Assign task) - Người nhận tự động lấy từ Token OIDC bảo mật của Backend
  assignTask: async (taskId: string, assignee: string): Promise<void> => {
    const response = await apiClient.post<BaseResponse<void>>(`/tasks/${taskId}/assign`);
    if (response.data.code !== '200') {
      throw new Error(response.data.message || 'Không thể nhận việc');
    }
  },

  // 4. Giải phóng/Hủy nhận việc (Unassign task) - Gọi endpoint unassign bảo mật ở Backend
  unassignTask: async (taskId: string): Promise<void> => {
    const response = await apiClient.post<BaseResponse<void>>(`/tasks/${taskId}/unassign`);
    if (response.data.code !== '200') {
      throw new Error(response.data.message || 'Không thể giải phóng việc');
    }
  },

  // 5. Hoàn thành task (Complete task)
  completeTask: async (taskId: string, decision: 'APPROVED' | 'REJECTED'): Promise<void> => {
    const response = await apiClient.post<BaseResponse<void>>(`/tasks/${taskId}/complete?decision=${decision}`);
    if (response.data.code !== '200') {
      throw new Error(response.data.message || 'Không thể hoàn thành công việc');
    }
  },

  // 6. Nộp đơn xin nghỉ phép kích hoạt quy trình mới
  submitLeaveRequest: async (formData: any): Promise<any> => {
    const response = await apiClient.post<BaseResponse<any>>('/leave-requests/submit', formData);
    if (response.data.code !== '200') {
      throw new Error(response.data.message || 'Gửi yêu cầu thất bại');
    }
    return response.data;
  },

  // 7. Đăng xuất hệ thống giải phóng cookie/session ở Backend
  logout: async (): Promise<void> => {
    await apiClient.post('/public/logout');
  },
};
