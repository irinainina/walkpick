// Deletes the account of whoever asks, and everything the server holds for it.
//
// A phone cannot do this itself: removing a user needs the service role key,
// which never leaves the server. The caller proves who they are with their own
// token and the function deletes only that user - it never takes an id from
// the request.
//
// No CORS headers on purpose: the only caller is the app, and with the headers
// a page in any browser could call this function too.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const BUCKET = "photos";
const PAGE = 1000;

function env(name: string): string {
  return (Deno.env.get(name) ?? "").trim();
}

function answer(status: number, body: Record<string, string>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response(null, { status: 204 });

  const token = request.headers.get("Authorization")?.replace(/^Bearer\s+/i, "");
  if (!token) return answer(401, { error: "no authorization header" });

  const admin = createClient(env("SUPABASE_URL"), env("SUPABASE_SERVICE_ROLE_KEY"));
  const { data, error } = await admin.auth.getUser(token);
  if (error || !data.user) return answer(401, { error: "not signed in" });
  const userId = data.user.id;

  // Photos go first: files are not rows, so no cascade reaches them, and after
  // the user is gone nobody could ask for them to be removed.
  while (true) {
    const page = await admin.storage.from(BUCKET).list(userId, { limit: PAGE });
    if (page.error) {
      console.error("the photos were not listed", page.error);
      return answer(502, { error: "the account was not deleted" });
    }
    if (page.data.length === 0) break;
    const removed = await admin.storage
      .from(BUCKET)
      .remove(page.data.map((file) => `${userId}/${file.name}`));
    if (removed.error) {
      console.error("the photos were not removed", removed.error);
      return answer(502, { error: "the account was not deleted" });
    }
  }

  // Every table hangs off auth.users with on delete cascade.
  const removed = await admin.auth.admin.deleteUser(userId);
  if (removed.error) {
    console.error("the account was not deleted", removed.error);
    return answer(502, { error: "the account was not deleted" });
  }

  return answer(200, { deleted: userId });
});
