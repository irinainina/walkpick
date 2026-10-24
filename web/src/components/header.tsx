"use client";

import { useEffect, useRef } from "react";
import { setLang, useLang } from "@/lib/lang";
import Logo from "./logo";

// While the site is a landing, the header leads to the stops of the home page
const LINKS = [
  ["walk", "Walk", "Прогулка"],
  ["photo", "Photo", "Фото"],
  ["health", "Health", "Здоровье"],
  ["results", "Results", "Результаты"],
  ["faq", "Questions", "Вопросы"],
];

/** A guest sees the language switch instead of the avatar. */
export default function Header({ guest = false }: { guest?: boolean }) {
  const lang = useLang();
  const header = useRef<HTMLElement>(null);

  // The header moves with the page: it leaves and comes back exactly as far as the page was scrolled
  useEffect(() => {
    const box = header.current!;
    let lastY = scrollY;
    let shift = 0;
    const place = (called: boolean) => {
      const reach = box.offsetHeight + 16;
      const footer = document.querySelector(".footer");
      if (footer && footer.getBoundingClientRect().top < reach + 32) shift = -reach;
      else shift = called ? 0 : Math.min(0, Math.max(-reach, shift - scrollY + lastY));
      lastY = scrollY;
      box.classList.toggle("header-called", called);
      box.style.transform = `translateY(${shift}px)`;
    };
    const scrolled = () => place(false);
    const moved = (event: MouseEvent) => {
      if (event.clientY < 24 && shift < 0) place(true);
    };
    addEventListener("scroll", scrolled);
    addEventListener("mousemove", moved);
    return () => {
      removeEventListener("scroll", scrolled);
      removeEventListener("mousemove", moved);
    };
  }, []);

  return (
    <header className="header" ref={header}>
      <div className="header-inner glass">
        <Logo />
        <nav className="nav">
          {LINKS.map(([id, en, ru]) => (
            <a key={id} href={`/#${id}`}>{lang === "ru" ? ru : en}</a>
          ))}
        </nav>
        {guest ? (
          <div className="lang" role="group" aria-label="Language">
            {(["en", "ru"] as const).map((code) => (
              <button key={code} type="button" aria-pressed={lang === code} onClick={() => setLang(code)}>{code.toUpperCase()}</button>
            ))}
          </div>
        ) : (
          <a className="avatar" href="/account.html" aria-label="Account">I</a>
        )}
      </div>
    </header>
  );
}
