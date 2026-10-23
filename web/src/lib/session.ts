// Signing in. The session is kept under the same key as in public/live.js, which reads it for the old pages.
export const API = "https://walkpick-406273949742.europe-west1.run.app/v1";
const SUPABASE = "https://qangmeeugxafvlfcuxyp.supabase.co";
// The publishable key: it is meant to be seen, the rows are guarded by the token of the user
const SUPABASE_KEY = "sb_publishable_D-RF7XFeTEqOB6HSxo2QCQ_PZLAK3ma";
const KEY = "session";
// A token this close to its end is changed for a new one before the request
const RENEW_MS = 60_000;

/** The demo session has no refresh token: public/live.js asks the service for a new one. */
interface Session {
  access: string;
  refresh: string | null;
  until: number;
}

const auth = async (path: string, body: object) => {
  const answer = await fetch(`${SUPABASE}/auth/v1/${path}`, {
    method: "POST",
    headers: { apikey: SUPABASE_KEY, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const said = await answer.json().catch(() => ({}));
  if (!answer.ok) throw new Error(said.msg || said.error_description || `Supabase: ${answer.status}`);
  return said;
};

const saved = (): Session | null => JSON.parse(localStorage.getItem(KEY) || "null");
const keep = (session: Session) => localStorage.setItem(KEY, JSON.stringify(session));
const keepSupabase = (got: { access_token: string; refresh_token: string; expires_in: number }) =>
  keep({ access: got.access_token, refresh: got.refresh_token, until: Date.now() + got.expires_in * 1000 });

export const signedIn = () => Boolean(saved());

export const sendCode = (email: string) => auth("otp", { email, create_user: false });

export const verifyCode = async (email: string, code: string) =>
  keepSupabase(await auth("verify", { type: "email", email, token: code }));

export const google = () => {
  const url = new URL(`${SUPABASE}/auth/v1/authorize`);
  url.search = new URLSearchParams({ provider: "google", redirect_to: `${location.origin}/sign-in` }).toString();
  location.assign(url);
};

/** Google sends the session back in the hash of the address, an error in the hash or in the query. */
export const takeGoogleAnswer = () => {
  if (!location.hash && !location.search.includes("error")) return;
  const got = new URLSearchParams(location.hash.slice(1) || location.search);
  history.replaceState(null, "", location.pathname);
  const error = got.get("error_description");
  if (error) throw new Error(error);
  const access = got.get("access_token");
  if (access)
    keepSupabase({ access_token: access, refresh_token: got.get("refresh_token")!, expires_in: Number(got.get("expires_in")) });
};

export const demo = async () => {
  const answer = await fetch(`${API}/demo/session`, { method: "POST" });
  if (!answer.ok) throw new Error(`Demo: ${answer.status}`);
  const got: { access_token: string; expires_at: number } = await answer.json();
  keep({ access: got.access_token, refresh: null, until: got.expires_at * 1000 });
};

/** The access token, renewed when it is about to end; null when there is no session to renew. */
export const token = async () => {
  const session = saved();
  if (!session) return null;
  if (session.until - Date.now() > RENEW_MS) return session.access;
  try {
    if (session.refresh) keepSupabase(await auth("token?grant_type=refresh_token", { refresh_token: session.refresh }));
    else await demo();
    return saved()!.access;
  } catch {
    return null;
  }
};

export const leave = () => {
  localStorage.removeItem(KEY);
  location.replace("/sign-in");
};
