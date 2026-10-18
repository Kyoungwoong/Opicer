import path from "node:path";
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Pin the workspace root to this app. Otherwise Next infers it from the
  // highest lockfile it finds, e.g. a stray ~/package-lock.json, and then
  // resolves/watches the whole home directory.
  turbopack: {
    root: path.resolve(__dirname),
  },
};

export default nextConfig;
