import Footer from "@/components/footer";
import Header from "@/components/header";

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <Header />
      <main className="app container">{children}</main>
      <Footer />
    </>
  );
}
