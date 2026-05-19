"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import axios from "axios";
import { toast } from "sonner";
import { Mail, Lock, User, Briefcase, KeyRound, ArrowRight, CheckCircle2 } from "lucide-react";
import { useRouter } from "next/navigation";
import Link from "next/link";

const step1Schema = z.object({
  fullName: z.string().min(1, "Họ tên không được để trống"),
  email: z.string().email("Email không hợp lệ").min(1, "Email không được để trống"),
  password: z.string().min(6, "Mật khẩu phải có ít nhất 6 ký tự"),
  groupId: z.string().min(1, "Vui lòng chọn bộ phận"),
});

const step2Schema = z.object({
  Otp: z.string().min(1, "OTP không được để trống"),
  email: z.string().email(),
});

type Step1Data = z.infer<typeof step1Schema>;
type Step2Data = z.infer<typeof step2Schema>;

const API_URL = "http://localhost:8081/api/v1/public";

export default function RegisterPage() {
  const [step, setStep] = useState<1 | 2>(1);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const step1Form = useForm<Step1Data>({
    resolver: zodResolver(step1Schema),
    defaultValues: {
      fullName: "",
      email: "",
      password: "",
      groupId: "9decc222-4f3a-44a8-91ea-534a8f1dcd6c", // Default: HR
    },
  });

  const step2Form = useForm<Step2Data>({
    resolver: zodResolver(step2Schema),
    defaultValues: {
      Otp: "",
      email: "",
    },
  });

  const onStep1Submit = async (data: Step1Data) => {
    setLoading(true);
    try {
      const response = await axios.post(`${API_URL}/register-step1`, {
        fullName: data.fullName,
        email: data.email,
        password: data.password,
        groupId: data.groupId,
      });

      if (response.data.code === "200") {
        toast.success("Đăng ký bước 1 thành công. Vui lòng kiểm tra email để lấy mã OTP.");
        step2Form.setValue("email", data.email);
        setStep(2);
      } else {
        toast.error(response.data.message || "Đăng ký thất bại");
      }
    } catch (error) {
      toast.error("Có lỗi xảy ra khi kết nối tới máy chủ.");
    } finally {
      setLoading(false);
    }
  };

  const onStep2Submit = async (data: Step2Data) => {
    setLoading(true);
    try {
      const response = await axios.post(`${API_URL}/register-step2`, {
        Otp: data.Otp,
        email: data.email,
      });

      if (response.data.code === "200") {
        toast.success("Xác thực thành công! Bạn có thể đăng nhập ngay bây giờ.");
        router.push("/login");
      } else {
        toast.error(response.data.message || "Xác thực OTP thất bại");
      }
    } catch (error) {
      toast.error("Có lỗi xảy ra khi kết nối tới máy chủ.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 p-4 relative overflow-hidden">
      {/* Background decorations */}
      <div className="absolute top-0 left-0 w-full h-full overflow-hidden z-0 pointer-events-none">
        <div className="absolute -top-[30%] -left-[10%] w-[70%] h-[70%] rounded-full bg-blue-900/20 blur-[120px]"></div>
        <div className="absolute -bottom-[30%] -right-[10%] w-[70%] h-[70%] rounded-full bg-indigo-900/20 blur-[120px]"></div>
      </div>

      <div className="w-full max-w-md bg-slate-900/80 backdrop-blur-xl border border-slate-800 p-8 rounded-3xl shadow-2xl z-10">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-blue-500/10 mb-4">
            {step === 1 ? (
              <User className="w-8 h-8 text-blue-500" />
            ) : (
              <CheckCircle2 className="w-8 h-8 text-green-500" />
            )}
          </div>
          <h1 className="text-3xl font-bold text-white mb-2">Tạo tài khoản</h1>
          <p className="text-slate-400 text-sm">
            {step === 1
              ? "Điền thông tin của bạn để tham gia hệ thống"
              : "Nhập mã OTP đã được gửi đến email của bạn"}
          </p>
        </div>

        {/* Form Step 1 */}
        {step === 1 && (
          <form onSubmit={step1Form.handleSubmit(onStep1Submit)} className="space-y-5">
            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-300 ml-1">Họ và tên</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <User className="h-5 w-5 text-slate-500" />
                </div>
                <input
                  {...step1Form.register("fullName")}
                  type="text"
                  placeholder="Nguyễn Văn A"
                  className="block w-full pl-10 pr-3 py-3 border border-slate-700 rounded-xl bg-slate-800/50 text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                />
              </div>
              {step1Form.formState.errors.fullName && (
                <p className="text-red-400 text-xs ml-1 mt-1">
                  {step1Form.formState.errors.fullName.message}
                </p>
              )}
            </div>

            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-300 ml-1">Email</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail className="h-5 w-5 text-slate-500" />
                </div>
                <input
                  {...step1Form.register("email")}
                  type="email"
                  placeholder="name@example.com"
                  className="block w-full pl-10 pr-3 py-3 border border-slate-700 rounded-xl bg-slate-800/50 text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                />
              </div>
              {step1Form.formState.errors.email && (
                <p className="text-red-400 text-xs ml-1 mt-1">
                  {step1Form.formState.errors.email.message}
                </p>
              )}
            </div>

            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-300 ml-1">Mật khẩu</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-slate-500" />
                </div>
                <input
                  {...step1Form.register("password")}
                  type="password"
                  placeholder="••••••••"
                  className="block w-full pl-10 pr-3 py-3 border border-slate-700 rounded-xl bg-slate-800/50 text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                />
              </div>
              {step1Form.formState.errors.password && (
                <p className="text-red-400 text-xs ml-1 mt-1">
                  {step1Form.formState.errors.password.message}
                </p>
              )}
            </div>

            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-300 ml-1">Bộ phận</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Briefcase className="h-5 w-5 text-slate-500" />
                </div>
                <select
                  {...step1Form.register("groupId")}
                  className="block w-full pl-10 pr-10 py-3 border border-slate-700 rounded-xl bg-slate-800/50 text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all appearance-none"
                >
                  <option value="9decc222-4f3a-44a8-91ea-534a8f1dcd6c">Phòng Nhân sự (HR)</option>
                  <option value="50010a19-c65f-452d-9785-92bdd8bf60e9">Phòng Nhân sự 1 (HR1)</option>
                </select>
                <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-4 text-slate-400">
                  <svg className="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z"/></svg>
                </div>
              </div>
              {step1Form.formState.errors.groupId && (
                <p className="text-red-400 text-xs ml-1 mt-1">
                  {step1Form.formState.errors.groupId.message}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-3.5 px-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-medium rounded-xl shadow-lg shadow-blue-500/25 transition-all duration-200 transform hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-70 disabled:cursor-not-allowed mt-2"
            >
              {loading ? (
                <span className="w-5 h-5 border-2 border-white/20 border-t-white rounded-full animate-spin"></span>
              ) : (
                <>
                  Tiếp tục
                  <ArrowRight className="w-5 h-5" />
                </>
              )}
            </button>
            
            <p className="text-center text-sm text-slate-400 mt-6">
              Đã có tài khoản?{" "}
              <Link href="/login" className="text-blue-400 hover:text-blue-300 font-medium transition-colors">
                Đăng nhập
              </Link>
            </p>
          </form>
        )}

        {/* Form Step 2 */}
        {step === 2 && (
          <form onSubmit={step2Form.handleSubmit(onStep2Submit)} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-500">
            <div className="bg-blue-500/10 border border-blue-500/20 rounded-xl p-4 mb-6">
              <p className="text-sm text-blue-200 text-center">
                Mã OTP đã được gửi đến email:<br/>
                <span className="font-semibold text-white">{step2Form.getValues("email")}</span>
              </p>
            </div>

            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-300 ml-1">Mã xác thực (OTP)</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <KeyRound className="h-5 w-5 text-slate-500" />
                </div>
                <input
                  {...step2Form.register("Otp")}
                  type="text"
                  placeholder="Nhập mã 6 số"
                  className="block w-full pl-10 pr-3 py-3 border border-slate-700 rounded-xl bg-slate-800/50 text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent transition-all tracking-widest text-center text-lg font-medium"
                  maxLength={6}
                />
              </div>
              {step2Form.formState.errors.Otp && (
                <p className="text-red-400 text-xs ml-1 mt-1 text-center">
                  {step2Form.formState.errors.Otp.message}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-3.5 px-4 bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-500 hover:to-emerald-500 text-white font-medium rounded-xl shadow-lg shadow-green-500/25 transition-all duration-200 transform hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-70 disabled:cursor-not-allowed mt-4"
            >
              {loading ? (
                <span className="w-5 h-5 border-2 border-white/20 border-t-white rounded-full animate-spin"></span>
              ) : (
                <>
                  Xác nhận
                  <CheckCircle2 className="w-5 h-5" />
                </>
              )}
            </button>
            
            <button
              type="button"
              onClick={() => setStep(1)}
              disabled={loading}
              className="w-full flex items-center justify-center py-3 px-4 bg-transparent hover:bg-slate-800 text-slate-400 hover:text-slate-200 font-medium rounded-xl transition-all duration-200"
            >
              Quay lại bước 1
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
