import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "WalkPick",
  description: "Your walks, photos and finds on a big screen.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
