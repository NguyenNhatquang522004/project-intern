'use client';

import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SessionProvider } from 'next-auth/react';
import { Toaster } from 'sonner';
import '@/app/globals.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="bg-slate-950" >
        <SessionProvider>
          <QueryClientProvider client={queryClient}>
            {children}
            <Toaster
              position="top-right"
              theme="dark"
              toastOptions={{
                style: {
                  background: '#0f172a',
                  border: '1px solid #1e293b',
                  color: '#f8fafc',
                }
              }}
            />
          </QueryClientProvider>
        </SessionProvider>
      </body>
    </html>
  );
}
