import "../../../public/hero.css";
import "../../../public/home-responsive.css";
import Footer from "@/components/footer";
import Header from "@/components/header";

export default function LegalLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <Header guest />
      {children}
      <Footer />
    </>
  );
}
