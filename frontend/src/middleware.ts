import { withAuth, NextRequestWithAuth } from "next-auth/middleware";
import { NextResponse } from "next/server";

/**
 * NextAuth Middleware: Bảo vệ các route cần xác thực
 *
 * Hoạt động:
 * 1. Kiểm tra cookie `next-auth.session-token` (HttpOnly, do NextAuth tạo)
 * 2. Nếu cookie không tồn tại hoặc hết hạn, NextAuth tự động redirect đến `/login`
 * 3. Nếu có session hợp lệ, cho phép tiếp tục
 *
 * Các route được bảo vệ:
 * - /dashboard/:path*
 * - /admin/:path*
 * - / (home page)
 *
 * Route không bảo vệ (public):
 * - /login
 * - /register
 * - /api/** (api routes)
 * - /_next/** (next.js internals)
 */
export default withAuth(
  function middleware(request: NextRequestWithAuth) {
    // Token đã được NextAuth xác thực từ cookie `next-auth.session-token`
    // request.nextauth.token sẽ null nếu cookie không tồn tại hoặc hết hạn
    const token = request.nextauth.token;

    // Nếu không có token, NextAuth tự động redirect đến /login
    // Nếu có token, tiếp tục
    return NextResponse.next();
  },
  {
    pages: {
      signIn: "/login",
    },
  }
);

export const config = {
  matcher: [
    "/",
    "/dashboard/:path*",
    "/admin/:path*",
    "/((?!login|register|api|_next/static|_next/image|favicon.ico|.well-known).*)",
  ],
};
