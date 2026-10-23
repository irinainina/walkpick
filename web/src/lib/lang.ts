// The language of the site, kept under the same key as in public/app.js, so the old pages agree.
import { useSyncExternalStore } from "react";

export type Lang = "en" | "ru";

const KEY = "lang";
const CHANGED = "lang-changed";

const read = (): Lang => {
  let stored = null;
  try {
    stored = localStorage.getItem(KEY);
  } catch {}
  if (stored === "en" || stored === "ru") return stored;
  return /^(ru|uk|be)/.test(navigator.language) ? "ru" : "en";
};

const subscribe = (changed: () => void) => {
  addEventListener(CHANGED, changed);
  return () => removeEventListener(CHANGED, changed);
};

export const useLang = () => useSyncExternalStore(subscribe, read, () => "en" as Lang);

export const setLang = (lang: Lang) => {
  try {
    localStorage.setItem(KEY, lang);
  } catch {}
  document.documentElement.lang = lang;
  dispatchEvent(new Event(CHANGED));
};
