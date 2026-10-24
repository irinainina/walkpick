import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  // The site is a landing for now: only the home page, privacy and terms are open.
  redirects: async () =>
    ["/overview", "/walks/:path*", "/sign-in", "/:page(account|health|photo|photo-page|walk-page).html"].map((source) => ({
      source,
      destination: "/",
      permanent: false,
    })),
  // The home page is still the static one from public/ until it is rebuilt in React.
  rewrites: async () => ({
    beforeFiles: [{ source: "/", destination: "/index.html" }],
  }),
};

export default nextConfig;
