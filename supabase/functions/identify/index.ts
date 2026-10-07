// Names the plant on a photo.
//
// The phone cannot ask Pl@ntNet itself: the key of the service would have to
// ship inside the app. The caller names a photo it has sent to the bucket; the
// function reads it with the caller's own token, so the policies of the bucket
// let it see only what belongs to the caller. It does not look at the finds
// table: a photo is named right after the shot, before its walk is saved.
//
// No CORS headers on purpose: the only caller is the app.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const BUCKET = "photos";
const PLANTNET = "https://my-api.plantnet.org/v2/identify/all";
// The best guess and three more for the "Not this?" sheet.
const GUESSES = 4;

type Guess = { name: string; latin_name: string; match: number };

function env(name: string): string {
  return (Deno.env.get(name) ?? "").trim();
}

function answer(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

async function plants(photo: Blob): Promise<Guess[] | null> {
  const form = new FormData();
  form.append("images", photo, "find.jpg");
  form.append("organs", "auto");
  const query = new URLSearchParams({
    "api-key": env("PLANTNET_API_KEY"),
    "lang": "en",
    "nb-results": String(GUESSES),
  });
  const reply = await fetch(`${PLANTNET}?${query}`, { method: "POST", body: form });
  // Pl@ntNet answers 404 when it sees no plant on the photo.
  if (reply.status === 404) return [];
  if (!reply.ok) {
    console.error("Pl@ntNet refused", reply.status, await reply.text());
    return null;
  }
  const found = await reply.json();
  // deno-lint-ignore no-explicit-any
  return (found.results ?? []).slice(0, GUESSES).map((result: any) => {
    const latin = result.species?.scientificNameWithoutAuthor ?? "";
    return {
      name: result.species?.commonNames?.[0] ?? latin,
      latin_name: latin,
      match: result.score ?? 0,
    };
  });
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response(null, { status: 204 });

  const authorization = request.headers.get("Authorization");
  if (!authorization) return answer(401, { error: "no authorization header" });
  const asked = await request.json().catch(() => null);
  if (typeof asked?.photo_path !== "string") return answer(400, { error: "no photo_path" });

  const caller = createClient(env("SUPABASE_URL"), env("SUPABASE_ANON_KEY"), {
    global: { headers: { Authorization: authorization } },
  });
  const photo = await caller.storage.from(BUCKET).download(asked.photo_path);
  if (photo.error) return answer(404, { error: "the photo is not on the server yet" });

  const guesses = await plants(photo.data);
  if (guesses === null) return answer(502, { error: "the service did not answer" });
  return answer(200, { guesses });
});
