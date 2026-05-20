"use client";

import { signOut, useSession } from "next-auth/react";
import { LogOut } from "lucide-react";

export default function LogoutButton() {
    const { data: session } = useSession();
    const handleLogout = async () => {

        // 👉 VIẾT NÓ NGAY TẠI ĐÂY 👈
        // Nhờ có bước khai báo mở rộng Type lúc nãy, session?.idToken sẽ không bị báo lỗi đỏ nữa
        const idToken = session?.idToken;

        // 2. Xóa sạch cụm cookie next-auth.session-token của Next.js (cổng 3000)
        await signOut({ redirect: false });

        // 3. Đường dẫn gọi sang server Keycloak để hủy nốt session tối cao
        const KEYCLOAK_LOGOUT_URL = "http://localhost:18080/auth/realms/camunda-platform/protocol/openid-connect/logout";
        const postLogoutRedirectUri = encodeURIComponent(window.location.origin);

        if (idToken) {
            // Nếu lấy được idToken thành công -> Ép Keycloak logout im lặng không hỏi lại
            window.location.href = `${KEYCLOAK_LOGOUT_URL}?id_token_hint=${idToken}&post_logout_redirect_uri=${postLogoutRedirectUri}`;
        } else {
            // Phương án dự phòng nếu không tìm thấy idToken
            const clientId = "cammuda-client";
            window.location.href = `${KEYCLOAK_LOGOUT_URL}?client_id=${clientId}&post_logout_redirect_uri=${postLogoutRedirectUri}`;
        }
    };
    return (
        <button
            type="button"
            onClick={() => handleLogout()}
            className="inline-flex items-center gap-2 rounded-full border border-slate-700 bg-slate-900/90 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 transition-colors"
        >
            <LogOut className="h-4 w-4" />
            Đăng xuất
        </button>
    );
}
