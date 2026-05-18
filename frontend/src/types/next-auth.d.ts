import NextAuth, { DefaultSession, DefaultUser } from "next-auth";
import { JWT } from "next-auth/jwt";

declare module "next-auth" {
  interface Session extends DefaultSession {
    accessToken?: string;
    error?: string;
    user: {
      id?: string;
      email?: string;
      name?: string;
      username?: string;
    } & DefaultSession["user"];
  }

  interface User extends DefaultUser {
    id?: string;
    username?: string;
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    idToken?: string;
    refreshToken?: string;
    expiresAt?: number;
    error?: string;
    username?: string;
  }
}
