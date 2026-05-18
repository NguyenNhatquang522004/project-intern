import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiService } from '@/services/api';
import { toast } from 'sonner';

export const useTasks = (filter: string = 'ALL_OPEN') => {
  const queryClient = useQueryClient();

  // 1. Fetch danh sách task theo bộ lọc hiện tại
  const tasksQuery = useQuery({
    queryKey: ['tasks', filter],
    queryFn: () => apiService.getTasks(filter),
    refetchInterval: 5000, // Tự động làm mới mỗi 5 giây
  });

  // 2. Fetch số lượng đếm thống kê của 4 bộ lọc
  const summaryQuery = useQuery({
    queryKey: ['filtersSummary'],
    queryFn: apiService.getFiltersSummary,
    refetchInterval: 5000,
  });

  // 3. Nhận việc (Assign Task)
  const assignMutation = useMutation({
    mutationFn: ({ taskId, assignee }: { taskId: string; assignee: string }) =>
      apiService.assignTask(taskId, assignee),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['filtersSummary'] });
      toast.success(`Đã nhận xử lý tác vụ thành công bởi ${variables.assignee}!`);
    },
    onError: (err: any) => {
      toast.error(`Không thể nhận việc: ${err.response?.data?.message || err.message}`);
    },
  });

  // 4. Hủy nhận việc (Unclaim/Unassign Task)
  const unassignMutation = useMutation({
    mutationFn: (taskId: string) => apiService.unassignTask(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['filtersSummary'] });
      toast.success('Đã giải phóng tác vụ thành công!');
    },
    onError: (err: any) => {
      toast.error(`Không thể giải phóng việc: ${err.response?.data?.message || err.message}`);
    },
  });

  // 5. Hoàn thành Task (Duyệt/Từ chối)
  const completeMutation = useMutation({
    mutationFn: ({ taskId, decision }: { taskId: string; decision: 'APPROVED' | 'REJECTED' }) =>
      apiService.completeTask(taskId, decision),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['filtersSummary'] });
      toast.success(
        variables.decision === 'APPROVED'
          ? 'Đã CHẤP THUẬN đơn nghỉ phép thành công!'
          : 'Đã TỪ CHỐI đơn nghỉ phép thành công!'
      );
    },
    onError: (err: any) => {
      toast.error(`Thao tác duyệt lỗi: ${err.response?.data?.message || err.message}`);
    },
  });

  return {
    tasks: tasksQuery.data || [],
    isLoading: tasksQuery.isLoading,
    error: tasksQuery.error,
    
    summary: summaryQuery.data || { allOpen: 0, assignedToMe: 0, unassigned: 0, completed: 0 },
    isLoadingSummary: summaryQuery.isLoading,

    assignTask: assignMutation.mutateAsync,
    unassignTask: unassignMutation.mutateAsync,
    completeTask: completeMutation.mutateAsync,
    
    isClaiming: assignMutation.isPending,
    isUnclaiming: unassignMutation.isPending,
    isCompleting: completeMutation.isPending,
  };
};
