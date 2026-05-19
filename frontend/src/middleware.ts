import { withAuth } from "next-auth/middleware";

/**
 * Next.js Middleware sử dụng NextAuth để tự động chặn các URL yêu cầu bảo mật.
 * Nếu chưa đăng nhập, NextAuth sẽ tự động chuyển hướng người dùng sang trang Login tùy biến.
 */
export default withAuth({
  pages: {
    signIn: "/login",
  },
});

export const config = {
  matcher: [
    // Bảo vệ toàn bộ console dashboard ngoại trừ login, register, api, static files, v.v.
    "/",
    "/((?!api|login|register|_next/static|_next/image|favicon.ico|.well-known).*)",
  ],
};
