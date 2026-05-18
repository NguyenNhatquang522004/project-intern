import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { TaskVariables } from '@/types/task';

// Định nghĩa schema kiểm tra hợp lệ bằng Zod
const formSchema = z.object({
  Email: z.string().email('Email không đúng định dạng').min(1, 'Email không được trống'),
  EmployeeID: z.string().uuid('Employee ID phải ở định dạng UUID hợp lệ').min(1, 'ID không được trống'),
  FullName: z.string().min(2, 'Họ tên phải từ 2 ký tự').max(50, 'Họ tên quá dài'),
  Department: z.enum(['IT', 'HR']),
  LeaveType: z.array(z.string()).min(1, 'Chọn ít nhất 1 loại nghỉ'),
  StartDate: z.string().min(1, 'Ngày bắt đầu không được để trống'),
  EndDate: z.string().min(1, 'Ngày kết thúc không được để trống'),
  leaveSession: z.enum(['MORNING', 'AFTERNOON', 'ALLDAY']),
  totaldays: z.number().min(0.5, 'Số ngày nghỉ tối thiểu là 0.5'),
  reason: z.string().max(250, 'Lý do tối đa 250 ký tự').optional().default(''),
  Status: z.enum(['PENDING', 'APPROVED', 'REJECTED']).default('PENDING'),
});

type FormValues = z.infer<typeof formSchema>;

interface DynamicTaskFormProps {
  mode: 'SUBMIT' | 'APPROVE_L1' | 'APPROVE_L2';
  initialData?: TaskVariables;
  onSubmit: (data: FormValues) => void;
  isSubmitting?: boolean;
}

export const DynamicTaskForm: React.FC<DynamicTaskFormProps> = ({
  mode,
  initialData,
  onSubmit,
  isSubmitting = false,
}) => {
  const isReadOnly = mode === 'APPROVE_L1' || mode === 'APPROVE_L2';

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(formSchema),
    defaultValues: {
      Email: 'hoa.bui@company.com',
      EmployeeID: 'b5090a5e-5be5-4547-b8c1-60297f4c1538',
      FullName: 'Bùi Thị Hoa',
      Department: 'HR',
      LeaveType: ['ANNUAL'],
      StartDate: '2026-05-21',
      EndDate: '2026-05-26',
      leaveSession: 'ALLDAY',
      totaldays: 5.5,
      reason: 'Nghỉ phép dài ngày',
      Status: 'PENDING',
    },
  });

  // Tải dữ liệu ban đầu nếu ở chế độ xem thông tin/phê duyệt
  useEffect(() => {
    if (initialData) {
      reset({
        Email: initialData.Email || '',
        EmployeeID: initialData.EmployeeID || '',
        FullName: initialData.FullName || '',
        Department: initialData.Department === 'HR' ? 'HR' : 'IT',
        LeaveType: Array.isArray(initialData.LeaveType)
          ? initialData.LeaveType
          : typeof initialData.LeaveType === 'string'
          ? [initialData.LeaveType]
          : ['ANNUAL'],
        StartDate: initialData.StartDate || '',
        EndDate: initialData.EndDate || '',
        leaveSession: initialData.leaveSession === 'ALL_DAY' ? 'ALLDAY' : initialData.leaveSession === 'MORNING' ? 'MORNING' : initialData.leaveSession === 'AFTERNOON' ? 'AFTERNOON' : 'ALLDAY',
        totaldays: initialData.totaldays || 0,
        reason: initialData.reason || '',
        Status: initialData.Status || 'PENDING',
      });
    }
  }, [initialData, reset]);

  // Tự động tính toán số ngày nghỉ khi thay đổi StartDate và EndDate
  const startDate = watch('StartDate');
  const endDate = watch('EndDate');
  const session = watch('leaveSession');

  useEffect(() => {
    if (startDate && endDate) {
      const start = new Date(startDate);
      const end = new Date(endDate);
      const diffTime = Math.abs(end.getTime() - start.getTime());
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
      
      let finalDays = isNaN(diffDays) ? 0 : diffDays;
      if (session === 'MORNING' || session === 'AFTERNOON') {
        finalDays = finalDays * 0.5;
      }
      setValue('totaldays', finalDays);
    }
  }, [startDate, endDate, session, setValue]);

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6 text-slate-100 max-w-3xl mx-auto p-2">
      {/* 1. Phần Thông Tin Nhân Viên */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-lg p-5 space-y-4">
        <h3 className="text-sm font-semibold text-sky-400 uppercase tracking-wider">Thông tin nhân viên</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs text-slate-400 mb-1">Email liên lạc</label>
            <input
              type="email"
              disabled={isReadOnly}
              {...register('Email')}
              className="w-full bg-slate-950 border border-slate-800 focus:border-sky-500 rounded px-3 py-2 text-sm transition focus:outline-none disabled:opacity-60 disabled:cursor-not-allowed"
            />
            {errors.Email && <span className="text-rose-500 text-xs mt-1">{errors.Email.message}</span>}
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Mã nhân viên (UUID)</label>
            <input
              type="text"
              disabled={isReadOnly}
              {...register('EmployeeID')}
              className="w-full bg-slate-950 border border-slate-800 focus:border-sky-500 rounded px-3 py-2 text-sm transition focus:outline-none disabled:opacity-60 disabled:cursor-not-allowed"
            />
            {errors.EmployeeID && <span className="text-rose-500 text-xs mt-1">{errors.EmployeeID.message}</span>}
          </div>

          <div className="md:col-span-2">
            <label className="block text-xs text-slate-400 mb-1">Họ và tên nhân viên</label>
            <input
              type="text"
              disabled={isReadOnly}
              {...register('FullName')}
              className="w-full bg-slate-950 border border-slate-800 focus:border-sky-500 rounded px-3 py-2 text-sm transition focus:outline-none disabled:opacity-60 disabled:cursor-not-allowed"
            />
            {errors.FullName && <span className="text-rose-500 text-xs mt-1">{errors.FullName.message}</span>}
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Phòng ban ban đầu</label>
            <select
              disabled={isReadOnly}
              {...register('Department')}
              className="w-full bg-slate-950 border border-slate-800 focus:border-sky-500 rounded px-3 py-2 text-sm transition focus:outline-none disabled:opacity-60 disabled:cursor-not-allowed"
            >
              <option value="IT">Công nghệ thông tin (IT)</option>
              <option value="HR">Nhân sự (HR)</option>
            </select>
            {errors.Department && <span className="text-rose-500 text-xs mt-1">{errors.Department.message}</span>}
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Loại ngày nghỉ phép</label>
            <div className="flex gap-4 items-center h-10 px-3 bg-slate-950/40 border border-slate-800/80 rounded">
              {['ANNUAL', 'SICK', 'UNPAID'].map((type) => (
                <label key={type} className="flex items-center gap-1.5 text-xs cursor-pointer select-none">
                  <input
                    type="checkbox"
                    value={type}
                    disabled={isReadOnly}
                    {...register('LeaveType')}
                    className="rounded text-sky-600 bg-slate-950 border-slate-800 focus:ring-sky-500 focus:ring-offset-slate-950 disabled:opacity-60"
                  />
                  {type}
                </label>
              ))}
            </div>
            {errors.LeaveType && <span className="text-rose-500 text-xs mt-1">{errors.LeaveType.message}</span>}
          </div>
        </div>
      </div>

      {/* 2. Phần Chi Tiết Đăng Ký Nghỉ */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-lg p-5 space-y-4">
        <h3 className="text-sm font-semibold text-sky-400 uppercase tracking-wider">Thời gian & Lý do nghỉ</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs text-slate-400 mb-1">Ngày bắt đầu nghỉ</label>
            <input
              type="date"
              disabled={isReadOnly}
              {...register('StartDate')}
              className="w-full bg-slate-950 border border-slate-800 focus:border-sky-500 rounded px-3 py-2 text-sm transition focus:outline-none disabled:opacity-60 disabled:cursor-not-allowed"
            />
            {errors.StartDate && <span className="text-rose-500 text-xs mt-1">{errors.StartDate.message}</span>}
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Ngày kết thúc nghỉ</label>
            <input
              type="date"
              disabled={isReadOnly}
              {...register('EndDate')}
              className="w-full bg-slate-950 border border-slate-800 focus:border-sky-500 rounded px-3 py-2 text-sm transition focus:outline-none disabled:opacity-60 disabled:cursor-not-allowed"
            />
            {errors.EndDate && <span className="text-rose-500 text-xs mt-1">{errors.EndDate.message}</span>}
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Ca đăng ký nghỉ</label>
            <div className="flex gap-4 items-center h-10 px-3 bg-slate-950/40 border border-slate-800/80 rounded">
              {[
                { label: 'Sáng', value: 'MORNING' },
                { label: 'Chiều', value: 'AFTERNOON' },
                { label: 'Cả ngày', value: 'ALLDAY' },
              ].map((item) => (
                <label key={item.value} className="flex items-center gap-1.5 text-xs cursor-pointer select-none">
                  <input
                    type="radio"
                    value={item.value}
                    disabled={isReadOnly}
                    {...register('leaveSession')}
                    className="text-sky-600 bg-slate-950 border-slate-800 focus:ring-sky-500 disabled:opacity-60"
                  />
                  {item.label}
                </label>
              ))}
            </div>
            {errors.leaveSession && <span className="text-rose-500 text-xs mt-1">{errors.leaveSession.message}</span>}
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Tổng số ngày tính toán</label>
            <input
              type="number"
              step="0.5"
              readOnly={true} // Bắt buộc readOnly thay vì disabled để React Hook Form có thể gửi giá trị đi
              {...register('totaldays', { valueAsNumber: true })}
              className="w-full bg-slate-950/80 border border-slate-800/80 rounded px-3 py-2 text-sm cursor-not-allowed opacity-80"
            />
          </div>

          <div className="md:col-span-2">
            <label className="block text-xs text-slate-400 mb-1">Lý do nghỉ chi tiết</label>
            <textarea
              rows={3}
              disabled={isReadOnly}
              {...register('reason')}
              className="w-full bg-slate-950 border border-slate-800 focus:border-sky-500 rounded px-3 py-2 text-sm transition focus:outline-none disabled:opacity-60 disabled:cursor-not-allowed"
              placeholder="Nhập lý do chi tiết..."
            />
            {errors.reason && <span className="text-rose-500 text-xs mt-1">{errors.reason.message}</span>}
          </div>
        </div>
      </div>

      {/* 3. Phần Quyết Định Phê Duyệt (Chỉ hiển thị chỉnh sửa cho người duyệt) */}
      {isReadOnly && (
        <div className="bg-sky-950/20 border border-sky-900/60 rounded-lg p-5 space-y-4">
          <h3 className="text-sm font-semibold text-sky-400 uppercase tracking-wider flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-sky-400 animate-ping"></span>
            Dành cho người duyệt phê duyệt
          </h3>
          <div>
            <label className="block text-xs text-slate-400 mb-2">Quyết định trạng thái đơn xin nghỉ</label>
            <div className="flex gap-6 items-center">
              {[
                { label: 'Phê duyệt (APPROVED)', value: 'APPROVED', colorClass: 'text-emerald-500 focus:ring-emerald-500' },
                { label: 'Từ chối (REJECTED)', value: 'REJECTED', colorClass: 'text-rose-500 focus:ring-rose-500' },
              ].map((item) => (
                <label key={item.value} className="flex items-center gap-2 text-sm font-medium cursor-pointer select-none text-slate-200">
                  <input
                    type="radio"
                    value={item.value}
                    {...register('Status')}
                    className={`h-4 w-4 bg-slate-950 border-slate-800 ${item.colorClass}`}
                  />
                  {item.label}
                </label>
              ))}
            </div>
            {errors.Status && <span className="text-rose-500 text-xs mt-1">{errors.Status.message}</span>}
          </div>
        </div>
      )}

      {/* 4. Tầng nút bấm điều hướng */}
      <div className="flex justify-end pt-4 border-t border-slate-800">
        {mode === 'SUBMIT' ? (
          <button
            type="submit"
            disabled={isSubmitting}
            className="px-6 py-2.5 bg-amber-500 hover:bg-amber-600 text-slate-950 font-semibold rounded text-sm transition focus:ring-2 focus:ring-amber-400 focus:ring-offset-2 focus:ring-offset-slate-950 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Đang kích hoạt quy trình...' : 'Kích hoạt quy trình nghỉ phép'}
          </button>
        ) : (
          <div className="flex gap-3">
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded text-sm transition focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2 focus:ring-offset-slate-950 disabled:opacity-50"
            >
              Xác nhận quyết định duyệt
            </button>
          </div>
        )}
      </div>
    </form>
  );
};
