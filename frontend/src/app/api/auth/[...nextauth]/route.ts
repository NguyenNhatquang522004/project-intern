import NextAuth, { NextAuthOptions } from 'next-auth';
import KeycloakProvider from 'next-auth/providers/keycloak';

export const authOptions: NextAuthOptions = {
  providers: [
    KeycloakProvider({
      clientId: process.env.KEYCLOAK_CLIENT_ID || 'orchestration',
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET || 'secret',
      issuer: process.env.KEYCLOAK_ISSUER || 'http://localhost:18080/auth/realms/camunda-platform',
    }),
  ],
  session: {
    strategy: 'jwt',
  },
  callbacks: {
    async jwt({ token, account, user }) {
      // Khi đăng nhập thành công lần đầu, đính kèm tokens vào JWT
      if (account && user) {
        token.accessToken = account.access_token;
        token.idToken = account.id_token;
        token.refreshToken = account.refresh_token;
        token.expiresAt = account.expires_at;
        token.username = token.name || (user as any).username;
      }
      return token;
    },
    async session({ session, token }) {
      // Đọc accessToken từ JWT lưu vào Next.js Session để truyền lên API Resource Server
      session.accessToken = token.accessToken;
      if (session.user) {
        session.user.username = token.username;
      }
      return session;
    },
  },
};

const handler = NextAuth(authOptions);
export { handler as GET, handler as POST };
