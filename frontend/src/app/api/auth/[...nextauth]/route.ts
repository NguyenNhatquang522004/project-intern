import NextAuth, { NextAuthOptions } from 'next-auth';
import KeycloakProvider from 'next-auth/providers/keycloak';
import CredentialsProvider from 'next-auth/providers/credentials';
import axios from 'axios';

const API_URL = "http://localhost:8081/api/v1/public";

async function refreshAccessToken(token: any) {
  try {
    const KEYCLOAK_TOKEN_URL = `${process.env.KEYCLOAK_ISSUER || 'http://localhost:18080/auth/realms/camunda-platform'}/protocol/openid-connect/token`;
    const params = new URLSearchParams();
    params.append('grant_type', 'refresh_token');
    params.append('client_id', process.env.KEYCLOAK_CLIENT_ID || '');
    params.append('client_secret', process.env.KEYCLOAK_CLIENT_SECRET || '');
    params.append('refresh_token', token.refreshToken || '');

    const response = await axios.post(KEYCLOAK_TOKEN_URL, params.toString(), {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });

    const refreshed = response.data;
    return {
      ...token,
      accessToken: refreshed.access_token,
      refreshToken: refreshed.refresh_token || token.refreshToken,
      expiresAt: Math.floor(Date.now() / 1000) + refreshed.expires_in,
      error: undefined,
    };
  } catch (error: any) {
    console.error('NextAuth [refreshAccessToken] error:', error);
    return {
      ...token,
      error: 'RefreshAccessTokenError',
    };
  }
}

export const authOptions: NextAuthOptions = {
  secret: process.env.NEXTAUTH_SECRET,
  providers: [
    KeycloakProvider({
      clientId: process.env.KEYCLOAK_CLIENT_ID || 'orchestration',
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET || 'secret',
      issuer: process.env.KEYCLOAK_ISSUER || 'http://localhost:18080/auth/realms/camunda-platform',
    }),
    CredentialsProvider({
      name: 'Credentials',
      credentials: {
        email: { label: "Email", type: "text" },
        password: { label: "Password", type: "password" }
      },
      async authorize(credentials) {
        if (!credentials?.email || !credentials?.password) {
          throw new Error("Vui lòng điền đầy đủ thông tin đăng nhập.");
        }

        try {
          const response = await axios.post(`${API_URL}/login`, {
            email: credentials.email,
            password: credentials.password
          });

          const data = response.data;

          if (data && data.code === "200" && data.data) {
            const loginData = data.data;
            console.log("NextAuth [Authorize] - Dữ liệu đăng nhập từ Backend:", loginData);

            const email = loginData.Email || loginData.email;
            const fullName = loginData.FullName || loginData.fullName || loginData.name;
            const accessToken = loginData.access_token || loginData.AccessToken || loginData.accessToken;
            const refreshToken = loginData.refresh_token || loginData.RefreshToken || loginData.refreshToken;
            const expiresIn = loginData.expires_in || loginData.expiresIn || loginData.expires_In || 3600;

            if (!accessToken) {
              console.error("NextAuth [Authorize] ERROR - access_token trống trong dữ liệu trả về:", loginData);
              throw new Error("Lỗi xác thực: Không nhận được Token từ hệ thống.");
            }

            return {
              id: loginData.id || email || "unknown",
              name: fullName || (email ? email.split('@')[0] : "User"),
              email: email,
              accessToken: accessToken,
              refreshToken: refreshToken,
              expiresAt: Math.floor(Date.now() / 1000) + Number(expiresIn)
            };
          } else {
            throw new Error(data.message || "Tên tài khoản hoặc mật khẩu không chính xác.");
          }
        } catch (error: any) {
          console.error("Authorize error:", error);
          throw new Error(error.response?.data?.message || error.message || "Không thể kết nối đến máy chủ.");
        }
      }
    })
  ],
  session: {
    strategy: 'jwt',
    maxAge: 60 * 60 * 24,
    updateAge: 60,
  },
  callbacks: {
    async jwt({ token, account, user }) {
      if (user) {
        token.id = user.id;
        token.accessToken = (user as any).accessToken || account?.access_token;
        token.idToken = (user as any).idToken || account?.id_token;
        token.refreshToken = (user as any).refreshToken || account?.refresh_token;
        token.expiresAt = (user as any).expiresAt || account?.expires_at;
        token.username = user.name || (user as any).username;
        token.email = user.email;
        console.log("NextAuth [JWT] - Khởi tạo token cho email:", token.email, "ID:", token.id, "Có accessToken:", !!token.accessToken);
      }

      const shouldRefresh = token.expiresAt && Date.now() / 1000 > (token.expiresAt as number) - 60;
      if (shouldRefresh) {
        return await refreshAccessToken(token);
      }

      return token;
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken as string;
      if (session.user) {
        session.user.id = token.id as string;
        session.user.username = token.username as string;
        session.user.email = token.email as string;
      }
      if (token.error) {
        session.error = token.error as string;
      }
      console.log("NextAuth [Session] - Xuất session cho email:", session.user?.email, "ID:", session.user?.id, "Có accessToken:", !!session.accessToken);
      return session;
    },
  },
  pages: {
    signIn: '/login',
  }
};

const handler = NextAuth(authOptions);
export { handler as GET, handler as POST };
