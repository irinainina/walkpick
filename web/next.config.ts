import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  // The home page is still the static one from public/ until it is rebuilt in React.
  rewrites: async () => ({
    beforeFiles: [{ source: "/", destination: "/index.html" }],
  }),
};

export default nextConfig;
