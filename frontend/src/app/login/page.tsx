"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { signIn } from "next-auth/react";
import axios from "axios";
import { toast } from "sonner";
import { Mail, Lock, ArrowRight, ArrowLeft, KeyRound, LogIn } from "lucide-react";
import { useRouter } from "next/navigation";
import Link from "next/link";

const loginSchema = z.object({
  email: z.string().email("Email không hợp lệ").min(1, "Email không được để trống"),
  password: z.string().min(1, "Mật khẩu không được để trống"),
});

const forgotPasswordSchema = z.object({
  email: z.string().email("Email không hợp lệ").min(1, "Email không được để trống"),
});

type LoginData = z.infer<typeof loginSchema>;
type ForgotPasswordData = z.infer<typeof forgotPasswordSchema>;

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081/api/v1/public";

export default function LoginPage() {
  const [view, setView] = useState<"login" | "forgot">("login");
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const loginForm = useForm<LoginData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  const forgotForm = useForm<ForgotPasswordData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: "",
    },
  });

  const onLoginSubmit = async (data: LoginData) => {
    setLoading(true);
    try {
      const result = await signIn("credentials", {
        redirect: false,
        email: data.email,
        password: data.password,
      });

      if (result?.error) {
        toast.error(result.error);
      } else {
        toast.success("Đăng nhập thành công!");
        router.push("/");
        router.refresh();
      }
    } catch (error) {
      toast.error("Có lỗi xảy ra trong quá trình đăng nhập.");
    } finally {
      setLoading(false);
    }
  };

  const onForgotSubmit = async (data: ForgotPasswordData) => {
    setLoading(true);
    try {
      const response = await axios.post(`${API_URL}/reset-password`, {
        email: data.email,
        clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || "orchestration",
        redirectUri: window.location.origin || "http://localhost:3000",
      });

      if (response.data.code === "200") {
        toast.success("Yêu cầu khôi phục mật khẩu thành công! Vui lòng kiểm tra email của bạn.");
        setView("login");
      } else {
        toast.error(response.data.message || "Gửi yêu cầu thất bại.");
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Có lỗi xảy ra khi kết nối tới máy chủ.");
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
            {view === "login" ? (
              <LogIn className="w-8 h-8 text-blue-500" />
            ) : (
              <KeyRound className="w-8 h-8 text-amber-500" />
            )}
          </div>
          <h1 className="text-3xl font-bold text-white mb-2">
            {view === "login" ? "Chào mừng trở lại" : "Khôi phục mật khẩu"}
          </h1>
          <p className="text-slate-400 text-sm">
            {view === "login"
              ? "Vui lòng đăng nhập để tiếp tục quản lý công việc"
              : "Chúng tôi sẽ gửi email chứa đường dẫn đặt lại mật khẩu"}
          </p>
        </div>

        {/* Login View */}
        {view === "login" && (
          <form onSubmit={loginForm.handleSubmit(onLoginSubmit)} className="space-y-5">
            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-300 ml-1">Email</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail className="h-5 w-5 text-slate-500" />
                </div>
                <input
                  {...loginForm.register("email")}
                  type="email"
                  placeholder="name@example.com"
                  className="block w-full pl-10 pr-3 py-3 border border-slate-700 rounded-xl bg-slate-800/50 text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                />
              </div>
              {loginForm.formState.errors.email && (
                <p className="text-red-400 text-xs ml-1 mt-1">
                  {loginForm.formState.errors.email.message}
                </p>
              )}
            </div>

            <div className="space-y-1">
              <div className="flex justify-between items-center px-1">
                <label className="text-sm font-medium text-slate-300">Mật khẩu</label>
                <button
                  type="button"
                  onClick={() => setView("forgot")}
                  className="text-xs text-blue-400 hover:text-blue-300 transition-colors"
                >
                  Quên mật khẩu?
                </button>
              </div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-slate-500" />
                </div>
                <input
                  {...loginForm.register("password")}
                  type="password"
                  placeholder="••••••••"
                  className="block w-full pl-10 pr-3 py-3 border border-slate-700 rounded-xl bg-slate-800/50 text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                />
              </div>
              {loginForm.formState.errors.password && (
                <p className="text-red-400 text-xs ml-1 mt-1">
                  {loginForm.formState.errors.password.message}
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
                  Đăng nhập
                  <ArrowRight className="w-5 h-5" />
                </>
              )}
            </button>

            <button
              type="button"
              onClick={() => signIn('keycloak')}
              className="w-full flex items-center justify-center gap-2 py-3.5 px-4 border border-slate-700 rounded-xl text-slate-100 hover:bg-slate-800 transition-all duration-200"
            >
              Đăng nhập bằng Keycloak
            </button>

            <p className="text-center text-sm text-slate-400 mt-6">
              Chưa có tài khoản?{' '}
              <Link href="/register" className="text-blue-400 hover:text-blue-300 font-medium transition-colors">
                Đăng ký ngay
              </Link>
            </p>
          </form>
        )}

        {/* Forgot Password View */}
        {view === "forgot" && (
          <form onSubmit={forgotForm.handleSubmit(onForgotSubmit)} className="space-y-5 animate-in fade-in slide-in-from-right-4 duration-500">
            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-300 ml-1">Email khôi phục</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail className="h-5 w-5 text-slate-500" />
                </div>
                <input
                  {...forgotForm.register("email")}
                  type="email"
                  placeholder="name@example.com"
                  className="block w-full pl-10 pr-3 py-3 border border-slate-700 rounded-xl bg-slate-800/50 text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all"
                />
              </div>
              {forgotForm.formState.errors.email && (
                <p className="text-red-400 text-xs ml-1 mt-1">
                  {forgotForm.formState.errors.email.message}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-3.5 px-4 bg-gradient-to-r from-amber-600 to-orange-600 hover:from-amber-500 hover:to-orange-500 text-white font-medium rounded-xl shadow-lg shadow-amber-500/25 transition-all duration-200 transform hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-70 disabled:cursor-not-allowed mt-2"
            >
              {loading ? (
                <span className="w-5 h-5 border-2 border-white/20 border-t-white rounded-full animate-spin"></span>
              ) : (
                <>
                  Gửi yêu cầu
                  <ArrowRight className="w-5 h-5" />
                </>
              )}
            </button>

            <button
              type="button"
              onClick={() => setView("login")}
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-3 px-4 bg-transparent hover:bg-slate-800 text-slate-400 hover:text-slate-200 font-medium rounded-xl transition-all duration-200"
            >
              <ArrowLeft className="w-4 h-4" />
              Quay lại đăng nhập
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
