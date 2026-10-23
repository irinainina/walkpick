"use client";

import { useLang } from "@/lib/lang";
import Logo from "./logo";

export default function Footer() {
  const ru = useLang() === "ru";
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <Logo />
        <ul className="footer-links">
          <li><a href="/privacy">{ru ? "Политика конфиденциальности" : "Privacy Policy"}</a></li>
          <li><a href="/terms">{ru ? "Условия использования" : "Terms of Use"}</a></li>
          <li>© 2026 WalkPick</li>
        </ul>
      </div>
    </footer>
  );
}
