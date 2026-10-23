"use client";

import { usePathname } from "next/navigation";
import { useEffect, useRef } from "react";
import { setLang, useLang } from "@/lib/lang";
import Logo from "./logo";

// The pages not yet rebuilt in React are the static ones from public/
const LINKS = [
  ["/overview", "Overview", "Обзор"],
  ["/walk.html", "Walks", "Прогулки"],
  ["/photo.html", "Photos", "Фото"],
  ["/health.html", "Health", "Здоровье"],
];

/** A guest sees the language switch and Sign in instead of the avatar. */
export default function Header({ guest = false }: { guest?: boolean }) {
  const path = usePathname();
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
          {LINKS.map(([href, en, ru]) => (
            <a key={href} href={href} aria-current={path === href ? "page" : undefined}>{lang === "ru" ? ru : en}</a>
          ))}
        </nav>
        {guest ? (
          <>
            <div className="lang" role="group" aria-label="Language">
              {(["en", "ru"] as const).map((code) => (
                <button key={code} type="button" aria-pressed={lang === code} onClick={() => setLang(code)}>{code.toUpperCase()}</button>
              ))}
            </div>
            <a className="button button-small button-ghost" href="/sign-in">{lang === "ru" ? "Войти" : "Sign in"}</a>
          </>
        ) : (
          <a className="avatar" href="/account.html" aria-label="Account">I</a>
        )}
      </div>
    </header>
  );
}
