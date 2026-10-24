import Footer from "@/components/footer";
import Header from "@/components/header";
import styles from "./account.module.css";

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className={styles.account}>
      <Header />
      {children}
      <Footer />
    </div>
  );
}
