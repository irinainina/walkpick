import Image from "next/image";
import "../../public/hero.css";
import "../../public/home-responsive.css";
import styles from "./hero.module.css";

/** The photo across the page with a glass plate: the title, then whatever the page puts under it. */
export default function Hero({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className={`hero ${styles.hero}`}>
      <Image className="hero-photo" src="/assets/images/hero.webp" alt="" fill priority sizes="100vw" />
      <div className="container hero-inner">
        <div className="hero-text glass">
          <h1 className="hero-title">{title}</h1>
          {children}
        </div>
      </div>
    </section>
  );
}
