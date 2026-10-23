"use client";

import Image from "next/image";
import { useLang } from "@/lib/lang";
import styles from "./legal.module.css";

interface Text {
  title: string;
  body: React.ReactNode;
}

/** A legal page: the photo with the title, then the text in the chosen language. */
export default function Legal({ en, ru }: { en: Text; ru: Text }) {
  const lang = useLang();
  const { title, body } = lang === "ru" ? ru : en;
  return (
    <>
      <section className={`hero ${styles.hero}`}>
        <Image className="hero-photo" src="/assets/images/hero.webp" alt="" fill priority sizes="100vw" />
        <div className="container hero-inner">
          <div className="hero-text glass">
            <h1 className="hero-title">{title}</h1>
            <p className="lead">{lang === "ru" ? "Обновлено 23 октября 2026" : "Last updated: October 23, 2026"}</p>
          </div>
        </div>
      </section>
      <main className={`container ${styles.legal}`}>{body}</main>
    </>
  );
}
