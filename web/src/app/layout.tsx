import type { Metadata } from "next";
import "../../public/style.css";
import "../../public/shell.css";
import "../../public/app.css";
import "../../public/pages.css";

export const metadata: Metadata = {
  title: "WalkPick",
  description: "Your walks, photos and finds on a big screen.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        {/* eslint-disable-next-line @next/next/no-page-custom-font -- the rule is for pages/, the root layout serves every page */}
        <link href="https://fonts.googleapis.com/css2?family=Golos+Text:wght@400;500;600&family=Literata:ital,opsz,wght@1,7..72,500&family=Manrope:wght@700;800&display=swap" rel="stylesheet" />
      </head>
      <body>{children}</body>
    </html>
  );
}
