"use client";

import { ReactNode } from "react";
import { SessionProvider } from "next-auth/react";

interface NextAuthProviderProps {
    children: ReactNode;
    session?: any;
}

export default function NextAuthProvider({ children, session }: NextAuthProviderProps) {
    return (
        <SessionProvider
            session={session}
            refetchOnWindowFocus={false}
            refetchInterval={5 * 60}
        >
            {children}
        </SessionProvider>
    );
}
