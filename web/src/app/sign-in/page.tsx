"use client";

import Image from "next/image";
import { useEffect, useState, type FormEvent } from "react";
import "../../../public/style.css";
import "../../../public/shell.css";
import "../../../public/app.css";
import "../../../public/pages.css";
import { demo, google, sendCode, signedIn, takeGoogleAnswer, verifyCode } from "@/lib/session";
import styles from "./sign-in.module.css";

// The pages of the account are still the static ones from public/
const ACCOUNT = "/walk.html";
// Offered while the address is typed: the part before "@" with the common mail services
const DOMAINS = ["gmail.com", "mail.ru", "yandex.ru", "outlook.com", "icloud.com"];

export default function SignIn() {
  const [email, setEmail] = useState("");
  const [typed, setTyped] = useState("");
  const [busy, setBusy] = useState(true);
  const [problem, setProblem] = useState("");

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setProblem("");
    try {
      await action();
    } catch (error) {
      setProblem((error as Error).message);
      setBusy(false);
    }
  };
  const enter = () => location.replace(ACCOUNT);
  const startDemo = () => run(async () => {
    await demo();
    enter();
  });
  const changeEmail = () => {
    setTyped(email);
    setEmail("");
    setProblem("");
  };
  const name = typed.split("@")[0];
  const hints = email || !name ? [] : DOMAINS.map(domain => `${name}@${domain}`).filter(hint => hint.startsWith(typed) && hint !== typed);

  // The home page sends "View demo" here, and Google brings the visitor back here
  useEffect(() => {
    const start = async () => {
      takeGoogleAnswer();
      if (new URLSearchParams(location.search).has("demo")) await demo();
      if (signedIn()) enter();
      else setBusy(false);
    };
    start().catch((error: Error) => {
      setProblem(error.message);
      setBusy(false);
    });
  }, []);

  // One field, two steps: the address first, then the code from the letter
  const submit = (event: FormEvent) => {
    event.preventDefault();
    const value = typed.trim();
    run(async () => {
      if (email) {
        await verifyCode(email, value);
        enter();
        return;
      }
      await sendCode(value);
      setEmail(value);
      setTyped("");
      setBusy(false);
    });
  };

  return (
    <>
      <header className="header">
        <div className="header-inner glass">
          <a className="logo" href="/">
            <svg className="logo-mark" viewBox="0 0 48 48" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
              <path d="M24 45s-15-12-15-24a15 15 0 0 1 30 0c0 12-15 24-15 24z" />
              <circle cx="24" cy="21" r="8.25" />
              <path fill="currentColor" fillRule="evenodd" stroke="none" transform="translate(24 21.5) scale(.75)" d="M6.3 -6.4C1 -6.6 -4.5 -5 -6 -1C-6.6 0.6 -6.3 1.8 -5.6 2.6L-4.9 3.7C-3 5 0.5 5.2 2.2 3.4C4.2 1.3 3 -3 6.3 -6.4ZM-3.72 2.77L-3.29 2.01L-2.81 1.22L-2.27 0.42L-1.66 -0.38L-0.99 -1.17L-0.23 -1.94L0.61 -2.66L1.55 -3.34A.55 .55 0 0 0 0.95 -4.26L-0.07 -3.53L-0.98 -2.74L-1.8 -1.91L-2.52 -1.07L-3.17 -0.22L-3.73 0.63L-4.24 1.45L-4.68 2.23A.55 .55 0 0 0 -3.72 2.77Z" />
            </svg>
            WalkPick
          </a>
        </div>
      </header>

      <main className={styles["sign-in"]}>
        <Image className={styles.photo} src="/assets/images/hero.webp" alt="Two people walking down a forest path into golden sunlight" fill priority sizes="100vw" />
        <div className={`${styles.form} glass`}>
          <div>
            <h1>{email ? "Check your email" : "Welcome back"}</h1>
            <p>{email ? `We sent a six-digit code to ${email}.` : "Your walks, photos and health notes on a big screen."}</p>
          </div>
          <button className="button button-ghost" type="button" disabled={busy} onClick={google}>
            <svg viewBox="0 0 24 24">
              <path fill="#4285f4" d="M21.6 12.2c0-.7-.1-1.3-.2-1.9H12v3.7h5.4a4.6 4.6 0 0 1-2 3v2.5h3.2c1.9-1.7 3-4.3 3-7.3z" />
              <path fill="#34a853" d="M12 22c2.7 0 5-.9 6.6-2.4l-3.2-2.5c-.9.6-2 1-3.4 1-2.6 0-4.8-1.8-5.6-4.1H3.1v2.6A10 10 0 0 0 12 22z" />
              <path fill="#fbbc05" d="M6.4 14c-.2-.6-.3-1.3-.3-2s.1-1.4.3-2V7.4H3.1a10 10 0 0 0 0 9.2z" />
              <path fill="#ea4335" d="M12 5.9c1.5 0 2.8.5 3.8 1.5l2.9-2.9A10 10 0 0 0 3.1 7.4L6.4 10c.8-2.3 3-4.1 5.6-4.1z" />
            </svg>
            Continue with Google
          </button>
          <p className={styles.or}>or</p>
          <form className="entry" onSubmit={submit}>
            <label className="field">
              <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M4 6h16v12H4z M4 7l8 6 8-6" />
              </svg>
              <input
                key={email ? "code" : "email"}
                name={email ? "code" : "email"}
                list={email ? undefined : "email-hints"}
                type={email ? "text" : "email"}
                inputMode={email ? "numeric" : "email"}
                autoComplete={email ? "one-time-code" : "email"}
                placeholder={email ? "Code from the email" : "Your email"}
                aria-label={email ? "Code" : "Email"}
                required
                value={typed}
                onChange={event => setTyped(event.target.value)}
              />
            </label>
            <datalist id="email-hints">
              {hints.map(hint => <option key={hint} value={hint} />)}
            </datalist>
            <button className="button" type="submit" disabled={busy}>{email ? "Sign in" : "Send me a code"}</button>
            <p className={styles.hint}>
              {problem || (email ? "No letter? Look in the spam folder." : "We will email a six-digit code. No password needed.")}
            </p>
            {email && <button className="link" type="button" onClick={changeEmail}>Use another email</button>}
          </form>
          <div className={styles.demo}>
            <button className="link" type="button" disabled={busy} onClick={startDemo}>
              View demo
              <svg className="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M7 17L17 7M9 7h8v8" />
              </svg>
            </button>
            <p className={styles.hint}>A sample account with a year of walks. View only.</p>
          </div>
        </div>
        <p className={`${styles.voice} voice`}>{"You set out in one mood.\nYou come back in a better one."}</p>
      </main>
    </>
  );
}
