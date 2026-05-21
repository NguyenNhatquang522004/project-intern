import NextAuth, { NextAuthOptions } from 'next-auth';
import KeycloakProvider from 'next-auth/providers/keycloak';
import CredentialsProvider from 'next-auth/providers/credentials';
import axios from 'axios';
import { authOptions } from '@/lib/auth';

// const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081/api/v1/public';
// const KEYCLOAK_ISSUER = process.env.KEYCLOAK_ISSUER || 'http://localhost:18080/auth/realms/camunda-platform';

// async function refreshAccessToken(token: any) {
//   try {
//     const tokenEndpoint = `${KEYCLOAK_ISSUER}/protocol/openid-connect/token`;
//     const params = new URLSearchParams();
//     params.append('grant_type', 'refresh_token');
//     params.append('client_id', process.env.KEYCLOAK_CLIENT_ID || 'camunda-client');
//     params.append('client_secret', process.env.KEYCLOAK_CLIENT_SECRET || 'sl9EiYyFEvzqsJonqhLlreJM2kbovtjp');
//     params.append('refresh_token', token.refreshToken || '');

//     const response = await axios.post(tokenEndpoint, params.toString(), {
//       headers: {
//         'Content-Type': 'application/x-www-form-urlencoded',
//       },
//     });

//     const refreshed = response.data;
//     return {
//       ...token,
//       accessToken: refreshed.access_token,
//       refreshToken: refreshed.refresh_token || token.refreshToken,
//       expiresAt: Math.floor(Date.now() / 1000) + refreshed.expires_in,
//       error: undefined,
//     };
//   } catch (error: any) {
//     console.error('NextAuth [refreshAccessToken] error:', error.response?.data || error.message || error);
//     return {
//       ...token,
//       error: 'RefreshAccessTokenError',
//     };
//   }
// }

// export const authOptions: NextAuthOptions = {
//   secret: process.env.NEXTAUTH_SECRET,
//   providers: [
//     KeycloakProvider({
//       clientId: process.env.KEYCLOAK_CLIENT_ID || 'cammuda-client',
//       clientSecret: process.env.KEYCLOAK_CLIENT_SECRET || 'sl9EiYyFEvzqsJonqhLlreJM2kbovtjp',
//       issuer: KEYCLOAK_ISSUER || 'http://localhost:18080/auth/realms/camunda-platform',
//       authorization: {
//         url: `${KEYCLOAK_ISSUER}/protocol/openid-connect/auth`,
//         params: {
//           scope: 'openid profile email offline_access',
//         },
    
//       },
//     }),
//     CredentialsProvider({
//       name: 'Credentials',
//       credentials: {
//         email: { label: 'Email', type: 'text' },
//         password: { label: 'Password', type: 'password' },
//       },
//       async authorize(credentials) {
//         if (!credentials?.email || !credentials?.password) {
//           throw new Error('Vui lòng điền đầy đủ thông tin đăng nhập.');
//         }

//         const response = await axios.post(`${API_URL}/login`, {
//           email: credentials.email,
//           password: credentials.password,
//         });

//         const data = response.data;
//         if (!data || data.code !== '200' || !data.data) {
//           throw new Error(data?.message || 'Tên tài khoản hoặc mật khẩu không chính xác.');
//         }

//         const loginData = data.data;
//         const email = loginData.Email || loginData.email || '';
//         const fullName = loginData.FullName || loginData.fullName || loginData.name || '';
//         const accessToken = loginData.access_token || loginData.AccessToken || loginData.accessToken;
//         const refreshToken = loginData.refresh_token || loginData.RefreshToken || loginData.refreshToken;
//         const expiresIn = loginData.expires_in || loginData.expiresIn || 3600;

//         if (!accessToken) {
//           throw new Error('Không nhận được access token từ backend.');
//         }

//         return {
//           id: loginData.id || email || 'user',
//           name: fullName || email.split('@')[0],
//           email,
//           accessToken,
//           refreshToken,
//           expiresAt: Math.floor(Date.now() / 1000) + Number(expiresIn),
//         };
//       },
//     }),
//   ],
//   session: {
//     strategy: 'jwt',
//     maxAge: 60 * 60 * 24,
//     updateAge: 60,
//   },
//   callbacks: {
//     async jwt({ token, account, user }) {
//       if (user) {
//         token.id = user.id;
//         token.accessToken = (user as any).accessToken || account?.access_token;
//         token.idToken = (user as any).idToken || account?.id_token;
//         token.refreshToken = (user as any).refreshToken || account?.refresh_token;
//         token.expiresAt = (user as any).expiresAt || account?.expires_at;
//         token.username = user.name || (user as any).username;
//         token.email = user.email;
//       }

//       if (token.expiresAt && Date.now() / 1000 > (token.expiresAt as number) - 60) {
//         if (!token.refreshToken) {
//           return {
//             ...token,
//             error: 'RefreshAccessTokenError',
//           };
//         }
//         return await refreshAccessToken(token);
//       }

//       return token;
//     },
//     async session({ session, token }) {
//       session.accessToken = token.accessToken as string;
//       if (session.user) {
//         session.user.id = token.id as string;
//         session.user.name = token.username as string;
//         session.user.email = token.email as string;
//       }
//       if (token.error) {
//         session.error = token.error as string;
//       }
//       return session;
//     },
//   },
//   pages: {
//     signIn: '/login',
//   },
// };

const handler = NextAuth(authOptions);
export { handler as GET, handler as POST };
