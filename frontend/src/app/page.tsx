'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useTasks } from '@/hooks/useTasks';
import { DynamicTaskForm } from '@/components/forms/DynamicTaskForm';
import { UserTask } from '@/types/task';
import { apiService } from '@/services/api';
import { useQueryClient } from '@tanstack/react-query';
import {
  Inbox,
  UserCheck,
  Users,
  Search,
  RefreshCw,
  Layers,
  Clock,
  User,
  AlertCircle,
  PlusCircle,
  X,
  CheckSquare,
  LogOut
} from 'lucide-react';
import { toast } from 'sonner';
import { useSession, signOut } from 'next-auth/react';

export default function TasklistDashboard() {
  const { data: session, status } = useSession();
  const router = useRouter();

  const handleLogout = async () => {
    try {
      // Xoá cookie phía client
      document.cookie = 'accessToken=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
      document.cookie = 'userEmail=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
      sessionStorage.removeItem('accessToken');
      await apiService.logout();
    } catch (err) {
      console.error('Đăng xuất backend thất bại:', err);
    } finally {
      signOut({ callbackUrl: '/login' });
    }
  };

  useEffect(() => {
    if (status === 'unauthenticated' || session?.error === 'RefreshAccessTokenError') {
      handleLogout();
    }
  }, [status, session, router]);

  // Đồng bộ accessToken với Cookie & sessionStorage để người dùng thấy/xoá thủ công
  const [isTokenSynced, setIsTokenSynced] = useState(false);

  useEffect(() => {
    if (session?.accessToken) {
      document.cookie = `accessToken=${session.accessToken}; path=/; max-age=86400; SameSite=Lax`;
      if (session.user?.email) {
        document.cookie = `userEmail=${session.user.email}; path=/; max-age=86400; SameSite=Lax`;
      }
      sessionStorage.setItem('accessToken', session.accessToken);
      setIsTokenSynced(true);
    }
  }, [session]);

  // Kiểm tra chu kỳ 2 giây xem token có bị xoá thủ công ở cookie không
  useEffect(() => {
    if (status !== 'authenticated' || !isTokenSynced) return;

    const checkToken = () => {
      const cookies = document.cookie.split(';').map(c => c.trim());
      const tokenCookie = cookies.find(c => c.startsWith('accessToken='));

      if (!tokenCookie) {
        console.warn("Phát hiện accessToken bị xóa thủ công! Đang tiến hành tự động đăng xuất...");
        handleLogout();
      }
    };

    const interval = setInterval(checkToken, 2000);
    return () => clearInterval(interval);
  }, [status, isTokenSynced]);

  const [activeFilter, setActiveFilter] = useState<string>('ALL_OPEN');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTask, setSelectedTask] = useState<UserTask | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isSubmittingNewRequest, setIsSubmittingNewRequest] = useState(false);

  const queryClient = useQueryClient();

  const {
    tasks,
    isLoading,
    summary,
    assignTask,
    unassignTask,
    completeTask,
    isClaiming,
    isUnclaiming,
    isCompleting
  } = useTasks(activeFilter);

  // Tên người dùng đăng nhập hiện tại làm người duyệt (đồng bộ động với backend qua OIDC token)
  const currentUser = session?.user?.email || session?.user?.username || '';

  // Kiểm tra UUID hợp lệ và lấy UUID của người dùng hiện tại
  const isUUID = (val: string) => /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(val);
  const userUuid = session?.user?.id && isUUID(session.user.id)
    ? session.user.id
    : '04a0c5a1-49fa-4606-b104-dbbc3ce6009e';

  // Kiểm tra tác vụ có thuộc về tài khoản hiện tại không (bảo vệ 100% khớp các định dạng từ Keycloak OIDC/Mock)
  const isAssignedToCurrentUser = (task: UserTask | null) => {
    if (!task || !task.assignee) return false;
    const assigneeLower = task.assignee.toLowerCase();
    const currentUsernameLower = (session?.user?.username || '').toLowerCase();
    const currentUserEmailLower = (session?.user?.email || '').toLowerCase();
    const emailPrefixLower = currentUserEmailLower.split('@')[0];

    return assigneeLower === currentUsernameLower ||
      assigneeLower === currentUserEmailLower ||
      assigneeLower === emailPrefixLower;
  };

  // Lọc danh sách tác vụ hiển thị theo từ khóa tìm kiếm
  const filteredTasks = tasks.filter((task) => {
    return task.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      task.id.includes(searchQuery) ||
      task.variables?.FullName?.toLowerCase().includes(searchQuery.toLowerCase());
  });

  // Tính toán thời gian tạo dạng thân thiện "2 hours ago"
  const getRelativeTime = (timeStr: string) => {
    if (!timeStr) return '';
    try {
      const created = new Date(timeStr);
      const now = new Date();
      const diffMs = now.getTime() - created.getTime();
      const diffMins = Math.floor(diffMs / 1000 / 60);
      const diffHours = Math.floor(diffMins / 60);
      const diffDays = Math.floor(diffHours / 24);

      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins} mins ago`;
      if (diffHours < 24) return `${diffHours} hours ago`;
      return `${diffDays} days ago`;
    } catch (e) {
      return timeStr;
    }
  };

  // Đồng bộ lại Task đã chọn khi danh sách cập nhật mới từ query
  const syncedSelectedTask = tasks.find(t => t.id === selectedTask?.id) || null;

  // Xử lý nộp đơn nghỉ phép mới kích hoạt Process Instance
  const handleCreateLeaveRequest = async (formData: any) => {
    setIsSubmittingNewRequest(true);
    try {
      // Map LeaveType từ array ["ANNUAL"] sang format list nếu backend mong đợi hoặc để nguyên
      const mappedData = {
        ...formData,
        BusinessKey: 'LR-' + Math.random().toString(36).substring(2, 9).toUpperCase(),
      };

      const response = await apiService.submitLeaveRequest(mappedData);
      toast.success(`Đơn xin nghỉ phép đã được nộp thành công! Instance Key: ${response.data || 'OK'}`);

      setIsCreateOpen(false);
      // Invalidate queries để tải lại danh sách task mới
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['filtersSummary'] });
    } catch (error: any) {
      console.error(error);
      toast.error(`Gửi đơn nghỉ phép thất bại: ${error.response?.data?.message || error.message}`);
    } finally {
      setIsSubmittingNewRequest(false);
    }
  };

  return (
    <div className="flex h-screen w-screen bg-slate-950 text-slate-100 overflow-hidden font-sans">

      {/* ========================================================
          1. SIDEBAR (Thanh bộ lọc bên trái - Camunda Style)
          ======================================================== */}
      <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between shrink-0">
        <div>
          {/* Logo Brand Camunda */}
          <div className="h-16 flex items-center gap-3 px-5 border-b border-slate-800/80">
            <div className="h-8 w-8 rounded bg-gradient-to-tr from-amber-500 to-amber-600 flex items-center justify-center font-bold text-slate-950 text-lg">
              C8
            </div>
            <div>
              <h1 className="font-bold text-sm tracking-wider text-white">CAMUNDA 8</h1>
              <p className="text-[10px] text-slate-400 font-mono tracking-widest uppercase">Tasklist Console</p>
            </div>
          </div>

          {/* Trigger tạo đơn xin nghỉ phép mới */}
          <div className="p-4 border-b border-slate-800/60">
            <button
              onClick={() => setIsCreateOpen(true)}
              className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-bold rounded text-xs transition active:scale-95 shadow"
            >
              <PlusCircle size={15} />
              <span>Nộp đơn nghỉ phép mới</span>
            </button>
          </div>

          {/* Bộ lọc Tác vụ */}
          <nav className="p-4 space-y-2">
            <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider px-2">Task Filters</span>

            <button
              onClick={() => {
                setActiveFilter('ALL_OPEN');
                setSelectedTask(null);
              }}
              className={`w-full flex items-center justify-between px-3 py-2 rounded text-sm transition font-medium ${activeFilter === 'ALL_OPEN'
                ? 'bg-slate-800 text-amber-500 border-l-4 border-amber-500'
                : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
                }`}
            >
              <div className="flex items-center gap-2.5">
                <Inbox size={16} />
                <span>All Open Tasks</span>
              </div>
              <span className="text-xs bg-slate-950 text-slate-400 font-mono px-2 py-0.5 rounded-full border border-slate-800">
                {summary.allOpen}
              </span>
            </button>

            <button
              onClick={() => {
                setActiveFilter('ASSIGNED_TO_ME');
                setSelectedTask(null);
              }}
              className={`w-full flex items-center justify-between px-3 py-2 rounded text-sm transition font-medium ${activeFilter === 'ASSIGNED_TO_ME'
                ? 'bg-slate-800 text-amber-500 border-l-4 border-amber-500'
                : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
                }`}
            >
              <div className="flex items-center gap-2.5">
                <UserCheck size={16} />
                <span>Assigned to Me</span>
              </div>
              <span className="text-xs bg-slate-950 text-slate-400 font-mono px-2 py-0.5 rounded-full border border-slate-800">
                {summary.assignedToMe}
              </span>
            </button>

            <button
              onClick={() => {
                setActiveFilter('UNASSIGNED');
                setSelectedTask(null);
              }}
              className={`w-full flex items-center justify-between px-3 py-2 rounded text-sm transition font-medium ${activeFilter === 'UNASSIGNED'
                ? 'bg-slate-800 text-amber-500 border-l-4 border-amber-500'
                : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
                }`}
            >
              <div className="flex items-center gap-2.5">
                <Users size={16} />
                <span>Unassigned Tasks</span>
              </div>
              <span className="text-xs bg-slate-950 text-slate-400 font-mono px-2 py-0.5 rounded-full border border-slate-800">
                {summary.unassigned}
              </span>
            </button>

            <button
              onClick={() => {
                setActiveFilter('COMPLETED');
                setSelectedTask(null);
              }}
              className={`w-full flex items-center justify-between px-3 py-2 rounded text-sm transition font-medium ${activeFilter === 'COMPLETED'
                ? 'bg-slate-800 text-amber-500 border-l-4 border-amber-500'
                : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
                }`}
            >
              <div className="flex items-center gap-2.5">
                <CheckSquare size={16} />
                <span>Completed Tasks</span>
              </div>
              <span className="text-xs bg-slate-950 text-slate-400 font-mono px-2 py-0.5 rounded-full border border-slate-800">
                {summary.completed}
              </span>
            </button>
          </nav>
        </div>

        {/* Thông tin User Tài khoản & Nút Đăng xuất */}
        <div className="p-4 border-t border-slate-800 bg-slate-950/40 flex items-center justify-between gap-2">
          <div className="flex items-center gap-3 overflow-hidden">
            <div className="h-9 w-9 rounded-full bg-slate-800 flex items-center justify-center text-sm font-semibold text-sky-400 border border-slate-700 shrink-0">
              {((session?.user?.username || session?.user?.name || currentUser || 'U') as string).charAt(0).toUpperCase()}
            </div>
            <div className="overflow-hidden">
              <p className="text-xs font-semibold text-slate-200 truncate">{session?.user?.username || session?.user?.name || "User Console"}</p>
              <p className="text-[10px] text-slate-500 truncate">{session?.user?.email || currentUser}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="p-2 text-slate-400 hover:text-red-400 hover:bg-red-500/10 rounded-xl transition-all duration-200 shrink-0"
            title="Đăng xuất"
          >
            <LogOut className="w-5 h-5" />
          </button>
        </div>
      </aside>

      {/* ========================================================
          2. TASK LIST PANE (Cột danh sách tác vụ ở giữa - 30%)
          ======================================================== */}
      <section className="w-[30%] bg-slate-950 border-r border-slate-800 flex flex-col shrink-0">
        {/* Thanh tìm kiếm & Reload */}
        <div className="h-16 px-4 border-b border-slate-800 flex items-center justify-between gap-2 bg-slate-900/30">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-2.5 text-slate-500" size={16} />
            <input
              type="text"
              placeholder="Search tasks..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 focus:border-amber-500/80 rounded pl-9 pr-3 py-1.5 text-xs transition focus:outline-none text-slate-200 placeholder-slate-500"
            />
          </div>
          <button
            onClick={() => {
              queryClient.invalidateQueries({ queryKey: ['tasks'] });
              queryClient.invalidateQueries({ queryKey: ['filtersSummary'] });
              toast.info('Đã tải lại danh sách tác vụ.');
            }}
            title="Reload Tasks"
            className="p-2 bg-slate-900 border border-slate-800 rounded text-slate-400 hover:text-amber-500 transition active:scale-95"
          >
            <RefreshCw size={14} />
          </button>
        </div>

        {/* Danh sách Task Cuộn độc lập */}
        <div className="flex-1 overflow-y-auto divide-y divide-slate-900 scrollbar-thin scrollbar-thumb-slate-800">
          {isLoading ? (
            Array.from({ length: 4 }).map((_, idx) => (
              <div key={idx} className="p-4 space-y-2 animate-pulse">
                <div className="h-4 bg-slate-800 rounded w-2/3"></div>
                <div className="h-3 bg-slate-900 rounded w-1/2"></div>
                <div className="flex gap-2">
                  <div className="h-5 bg-slate-900 rounded-full w-16"></div>
                  <div className="h-5 bg-slate-900 rounded-full w-24"></div>
                </div>
              </div>
            ))
          ) : filteredTasks.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center p-8 text-center text-slate-500">
              <Inbox size={40} className="mb-2 text-slate-700" />
              <p className="text-sm font-medium">No tasks available</p>
              <p className="text-xs text-slate-600 mt-1">Try changing your filters or searching.</p>
            </div>
          ) : (
            filteredTasks.map((task) => {
              const isSelected = selectedTask?.id === task.id;
              return (
                <div
                  key={task.id}
                  onClick={() => setSelectedTask(task)}
                  className={`p-4 cursor-pointer transition relative ${isSelected
                    ? 'bg-slate-900/80 border-l-4 border-amber-500'
                    : 'hover:bg-slate-900/40'
                    }`}
                >
                  <div className="flex justify-between items-start gap-2">
                    <span className="font-semibold text-sm text-slate-200">{task.name}</span>
                    <span className="text-[10px] text-slate-500 shrink-0 font-mono flex items-center gap-1">
                      <Clock size={10} />
                      {getRelativeTime(task.creationTime)}
                    </span>
                  </div>

                  <p className="text-xs text-slate-400 mt-1 flex items-center gap-1">
                    <Layers size={10} className="text-slate-600" />
                    BPMN Process: <span className="text-sky-500/80 font-mono font-semibold">{task.processId}</span>
                  </p>

                  <div className="flex gap-2 mt-3 flex-wrap">
                    {/* Badge Trạng thái gán việc */}
                    {task.assignee ? (
                      <span className="text-[10px] bg-slate-800 text-sky-400 border border-slate-700 px-2 py-0.5 rounded flex items-center gap-1 font-medium">
                        <User size={10} />
                        {isAssignedToCurrentUser(task) ? 'You' : task.assignee.split('@')[0]}
                      </span>
                    ) : (
                      <span className="text-[10px] bg-amber-500/10 text-amber-500 border border-amber-500/20 px-2 py-0.5 rounded font-medium">
                        Unassigned
                      </span>
                    )}
                    {/* Badge Trạng thái nghiệp vụ hiện tại */}
                    <span className={`text-[10px] px-2 py-0.5 rounded border font-medium ${task.variables?.Status === 'APPROVED'
                      ? 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20'
                      : task.variables?.Status === 'REJECTED'
                        ? 'bg-rose-500/10 text-rose-500 border-rose-500/20'
                        : 'bg-slate-800 text-slate-400 border-slate-700'
                      }`}>
                      {task.variables?.Status || 'PENDING'}
                    </span>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </section>

      {/* ========================================================
          3. TASK DETAIL PANE (Chi tiết tác vụ bên phải - 50%)
          ======================================================== */}
      <main className="flex-1 bg-slate-900/30 flex flex-col overflow-hidden">
        {syncedSelectedTask ? (
          <div className="h-full flex flex-col overflow-hidden">
            {/* Header Vùng chi tiết */}
            <div className="h-16 px-6 border-b border-slate-800 flex items-center justify-between bg-slate-900/50 shrink-0">
              <div>
                <h2 className="font-bold text-base text-white">{syncedSelectedTask.name}</h2>
                <div className="flex items-center gap-4 text-[10px] text-slate-400 font-mono mt-0.5">
                  <span>Instance Key: <strong className="text-slate-300">{syncedSelectedTask.processInstanceKey}</strong></span>
                  <span>Form Link: <strong className="text-sky-400">{syncedSelectedTask.formId}</strong></span>
                </div>
              </div>

              {/* Nhận / Hủy nhận công việc */}
              <div className="flex gap-2">
                {isAssignedToCurrentUser(syncedSelectedTask) ? (
                  <button
                    onClick={() => unassignTask(syncedSelectedTask.id)}
                    disabled={isUnclaiming}
                    className="px-3.5 py-1.5 bg-slate-800 border border-slate-700 hover:bg-slate-700/80 text-xs font-semibold rounded transition disabled:opacity-50"
                  >
                    {isUnclaiming ? 'Unclaiming...' : 'Unclaim Task'}
                  </button>
                ) : (
                  <button
                    onClick={() => assignTask({ taskId: syncedSelectedTask.id, assignee: currentUser })}
                    disabled={isClaiming || !!syncedSelectedTask.assignee}
                    className="px-3.5 py-1.5 bg-amber-500 hover:bg-amber-600 text-slate-950 text-xs font-bold rounded transition disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isClaiming ? 'Claiming...' : syncedSelectedTask.assignee ? 'Claimed' : 'Claim Task'}
                  </button>
                )}
              </div>
            </div>

            {/* Vùng Render Biểu mẫu (Form Body) */}
            <div className="flex-1 overflow-y-auto p-6 scrollbar-thin scrollbar-thumb-slate-800 bg-slate-950/20">
              {!isAssignedToCurrentUser(syncedSelectedTask) && (
                <div className="max-w-3xl mx-auto mb-6 p-4 bg-amber-500/10 border border-amber-500/20 rounded-lg flex gap-3 text-amber-500">
                  <AlertCircle size={20} className="shrink-0 mt-0.5" />
                  <div>
                    <h4 className="text-xs font-bold uppercase tracking-wider">Tác vụ đang ở trạng thái Read-only</h4>
                    <p className="text-xs text-slate-300 mt-1 leading-relaxed">
                      Bạn phải nhấn nút <strong>Claim Task</strong> ở góc phải phía trên để nhận xử lý công việc trước khi có thể phê duyệt hoặc thay đổi quyết định trạng thái.
                    </p>
                  </div>
                </div>
              )}

              <DynamicTaskForm
                mode={syncedSelectedTask.name.includes('2') ? 'APPROVE_L2' : 'APPROVE_L1'}
                initialData={syncedSelectedTask.variables}
                onSubmit={async (data) => {
                  if (!isAssignedToCurrentUser(syncedSelectedTask)) {
                    toast.error('Vui lòng nhận việc (Claim Task) trước khi xác nhận duyệt!');
                    return;
                  }
                  await completeTask({
                    taskId: syncedSelectedTask.id,
                    decision: data.Status as 'APPROVED' | 'REJECTED',
                  });
                  setSelectedTask(null);
                }}
                isSubmitting={isCompleting}
              />
            </div>
          </div>
        ) : (
          // Màn hình chào ban đầu
          <div className="h-full flex flex-col items-center justify-center text-slate-500 bg-slate-950/10">
            <Inbox size={56} className="mb-3 text-slate-800" />
            <h3 className="text-sm font-semibold text-slate-400">Select a task from the list</h3>
            <p className="text-xs text-slate-600 mt-1">Select any open task from the list to view form and execute approval workflow.</p>
          </div>
        )}
      </main>

      {/* ========================================================
          4. MODAL NỘP ĐƠN NGHỈ PHÉP MỚI
          ======================================================== */}
      {isCreateOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl w-full max-w-3xl shadow-2xl flex flex-col max-h-[90vh] overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <PlusCircle className="text-amber-500" size={18} />
                <h2 className="text-base font-bold text-white">Nộp Đơn Xin Nghỉ Phép Mới</h2>
              </div>
              <button
                onClick={() => setIsCreateOpen(false)}
                className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-slate-200 rounded transition"
              >
                <X size={16} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6 scrollbar-thin scrollbar-thumb-slate-800 bg-slate-950/20">
              <DynamicTaskForm
                mode="SUBMIT"
                onSubmit={handleCreateLeaveRequest}
                isSubmitting={isSubmittingNewRequest}
              />
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
