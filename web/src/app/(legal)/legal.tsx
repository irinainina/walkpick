"use client";

import Hero from "@/components/hero";
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
      <Hero title={title}>
        <p className="lead">{lang === "ru" ? "Обновлено 23 октября 2026" : "Last updated: October 23, 2026"}</p>
      </Hero>
      <main className={`container ${styles.legal}`}>{body}</main>
    </>
  );
}
