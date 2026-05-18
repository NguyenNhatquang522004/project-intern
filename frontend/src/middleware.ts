import { withAuth } from "next-auth/middleware";

/**
 * Next.js Middleware sử dụng NextAuth để tự động chặn các URL yêu cầu bảo mật.
 * Nếu chưa đăng nhập, NextAuth sẽ tự động chuyển hướng người dùng sang trang Keycloak Login.
 */
export default withAuth({
  pages: {
    signIn: "/api/auth/signin", // Chuyển hướng OIDC Keycloak chuẩn
  },
});

export const config = {
  matcher: [
    // Bảo vệ toàn bộ giao diện điều khiển của Console Dashboard

    "/",
    "/((?!api/auth|_next/static|_next/image|favicon.ico).*)",
    "/((?!api|_next/static|_next/image|favicon.ico|.well-known).*)",
  ],
};
