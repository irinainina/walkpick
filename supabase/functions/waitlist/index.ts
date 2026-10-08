// Takes an email from the "Join the waitlist" form of the landing.
//
// The landing is a static page: it cannot write to the database without a key
// in its script. The function holds the service role key and does one thing -
// adds an address to the waitlist table. An address left twice is kept once.
//
// Deployed without JWT verification: the caller is a visitor's browser.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SITE = "https://walkspot-landing.vercel.app";
// The longest address RFC 5321 allows.
const MAX_EMAIL = 254;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const cors = {
  "Access-Control-Allow-Origin": SITE,
  "Access-Control-Allow-Headers": "content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function env(name: string): string {
  return (Deno.env.get(name) ?? "").trim();
}

function answer(status: number, body: Record<string, string>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...cors, "Content-Type": "application/json" },
  });
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response(null, { headers: cors });
  if (request.method !== "POST") return answer(405, { error: "method" });

  const body = await request.json().catch(() => null);
  const email = String(body?.email ?? "").trim().toLowerCase();
  if (email.length > MAX_EMAIL || !EMAIL.test(email)) return answer(400, { error: "email" });

  const admin = createClient(env("SUPABASE_URL"), env("SUPABASE_SERVICE_ROLE_KEY"));
  const saved = await admin.from("waitlist").upsert({ email }, { onConflict: "email", ignoreDuplicates: true });
  if (saved.error) {
    console.error("the address was not saved", saved.error);
    return answer(500, { error: "server" });
  }
  return answer(200, { status: "ok" });
});
