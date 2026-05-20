import { getServerSession } from 'next-auth';
import { redirect } from 'next/navigation';
import { authOptions } from '@/lib/auth';
import LogoutButton from '@/components/LogoutButton';

/**
 * Dashboard Page (Server Component)
 *
 * Hoạt động:
 * 1. Gọi getServerSession(authOptions) trên Server để kiểm tra cookie `next-auth.session-token`
 * 2. Nếu session không tồn tại → redirect('/login')
 * 3. Nếu session tồn tại → Render dashboard với thông tin user
 *
 * Lợi ích Server Component:
 * - Kiểm tra auth trực tiếp trên server (an toàn hơn)
 * - Không lộ thông tin user qua client JavaScript
 * - Middleware + getServerSession = bảo vệ hai lớp
 */

interface PageProps {
    params?: { [key: string]: string | string[] };
    searchParams?: { [key: string]: string | string[] | undefined };
}

export default async function DashboardPage(props: PageProps) {
    // Kiểm tra session trên Server
    const session = await getServerSession(authOptions);

    // Nếu không có session, redirect đến login
    if (!session?.user) {
        redirect('/login');
    }

    // Tại đây, session đã được xác thực
    const user = session.user;
    const accessToken = session.accessToken;

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800">
            {/* Header */}
            <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-bold text-white">Dashboard</h1>
                        <p className="text-slate-400 text-sm mt-1">Chào mừng trở lại, {user?.name}</p>
                    </div>
                    <LogoutButton />
                </div>
            </header>

            {/* Main Content */}
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
                {/* User Info Card */}
                <div className="bg-slate-800/50 border border-slate-700 rounded-lg p-6 mb-8 backdrop-blur-sm">
                    <h2 className="text-lg font-semibold text-white mb-4">Thông tin người dùng</h2>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div className="bg-slate-900/50 rounded p-4">
                            <p className="text-slate-400 text-sm mb-1">Email</p>
                            <p className="text-white font-medium break-all">{user?.email}</p>
                        </div>
                        <div className="bg-slate-900/50 rounded p-4">
                            <p className="text-slate-400 text-sm mb-1">Tên</p>
                            <p className="text-white font-medium">{user?.name}</p>
                        </div>
                        <div className="bg-slate-900/50 rounded p-4">
                            <p className="text-slate-400 text-sm mb-1">ID</p>
                            <p className="text-white font-medium break-all text-sm">{user?.id}</p>
                        </div>
                    </div>
                </div>

                {/* API Call Section */}
                <div className="bg-slate-800/50 border border-slate-700 rounded-lg p-6 backdrop-blur-sm">
                    <h2 className="text-lg font-semibold text-white mb-4">Gọi API (Client Component Demo)</h2>
                    <p className="text-slate-400 mb-4">
                        Access Token đã có sẵn từ session. Bạn có thể sử dụng nó để gọi các API bảo vệ từ Client Component:
                    </p>
                    <div className="bg-slate-900/70 rounded p-4 mb-4 border border-slate-700">
                        <p className="text-slate-300 text-xs font-mono break-all">
                            {accessToken ? `${accessToken.slice(0, 50)}...` : 'Không có access token'}
                        </p>
                    </div>
                    <p className="text-slate-400 text-sm">
                        Trong Client Component, sử dụng `useSession()` từ NextAuth để lấy access token và gọi API backend.
                    </p>
                </div>

                {/* Info Section */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mt-8">
                    <div className="bg-blue-900/20 border border-blue-800 rounded-lg p-6">
                        <h3 className="text-lg font-semibold text-blue-300 mb-3">🔒 Bảo mật</h3>
                        <ul className="text-slate-300 text-sm space-y-2">
                            <li>✓ Middleware kiểm tra cookie trước khi render</li>
                            <li>✓ getServerSession xác thực trên Server</li>
                            <li>✓ Token refresh tự động khi hết hạn</li>
                            <li>✓ HttpOnly cookie không bị XSS truy cập</li>
                        </ul>
                    </div>
                    <div className="bg-green-900/20 border border-green-800 rounded-lg p-6">
                        <h3 className="text-lg font-semibold text-green-300 mb-3">✨ Tính năng</h3>
                        <ul className="text-slate-300 text-sm space-y-2">
                            <li>✓ SSO qua Keycloak</li>
                            <li>✓ Đăng nhập credentials (email + password)</li>
                            <li>✓ Refresh token tự động</li>
                            <li>✓ Logout với federated logout</li>
                        </ul>
                    </div>
                </div>
            </main>
        </div>
    );
}    