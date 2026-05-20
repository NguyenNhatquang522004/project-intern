import type { NextConfig } from "next";
const appId = process.env.NEXT_PUBLIC_APP_ID || 'default';
const nextConfig: NextConfig = {
  /* config options here */
  distDir: `.next-${appId}`,
  reactCompiler: true,
};

export default nextConfig;
