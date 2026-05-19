import NextAuth, { NextAuthOptions } from 'next-auth';
import KeycloakProvider from 'next-auth/providers/keycloak';
import CredentialsProvider from 'next-auth/providers/credentials';
import axios from 'axios';

const API_URL = "http://localhost:8081/api/v1/public";

export const authOptions: NextAuthOptions = {
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
            return {
              id: loginData.Email, // using email as ID
              name: loginData.FullName || loginData.Email.split('@')[0],
              email: loginData.Email,
              accessToken: loginData.access_token,
              refreshToken: loginData.refresh_token,
              expiresAt: Math.floor(Date.now() / 1000) + loginData.expires_in
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
  },
  callbacks: {
    async jwt({ token, account, user }) {
      if (user) {
        token.accessToken = (user as any).accessToken || account?.access_token;
        token.idToken = (user as any).idToken || account?.id_token;
        token.refreshToken = (user as any).refreshToken || account?.refresh_token;
        token.expiresAt = (user as any).expiresAt || account?.expires_at;
        token.username = user.name || (user as any).username;
        token.email = user.email;
      }
      return token;
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken as string;
      if (session.user) {
        session.user.username = token.username as string;
        session.user.email = token.email as string;
      }
      return session;
    },
  },
  pages: {
    signIn: '/login', // redirect to custom login page
  }
};

const handler = NextAuth(authOptions);
export { handler as GET, handler as POST };
