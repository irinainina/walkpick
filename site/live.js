// The account as the service gives it: the session, the requests and the rows the pages draw.
(() => {
  const API = 'https://walkpick-406273949742.europe-west1.run.app/v1';
  const SUPABASE = 'https://qangmeeugxafvlfcuxyp.supabase.co';
  // The publishable key: it is meant to be seen, the rows are guarded by the token of the user
  const SUPABASE_KEY = 'sb_publishable_D-RF7XFeTEqOB6HSxo2QCQ_PZLAK3ma';
  const SIGN_IN = 'sign-in.html';
  // A token this close to its end is changed for a new one before the request
  const RENEW_MS = 60_000;

  const auth = async (path, body) => {
    const answer = await fetch(`${SUPABASE}/auth/v1/${path}`, {
      method: 'POST',
      headers: { apikey: SUPABASE_KEY, 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    const said = await answer.json().catch(() => ({}));
    if (!answer.ok) throw new Error(said.msg || said.error_description || `Supabase: ${answer.status}`);
    return said;
  };
  const saved = () => JSON.parse(localStorage.getItem('session') || 'null');
  const keep = session => localStorage.setItem('session', JSON.stringify({
    access: session.access_token, refresh: session.refresh_token, until: Date.now() + session.expires_in * 1000,
  }));
  const leave = () => {
    localStorage.removeItem('session');
    location.replace(SIGN_IN);
  };
  // The session ends on the server too, so the token kept by a copied link stops working
  const signOut = async () => {
    const session = saved();
    localStorage.removeItem('session');
    if (session) {
      await fetch(`${SUPABASE}/auth/v1/logout?scope=local`, {
        method: 'POST',
        headers: { apikey: SUPABASE_KEY, Authorization: `Bearer ${session.access}` },
      }).catch(() => {});
    }
    location.replace('index.html');
  };
  const google = () => {
    const back = new URL('walk.html', location.href).href;
    location.href = `${SUPABASE}/auth/v1/authorize?provider=google&redirect_to=${encodeURIComponent(back)}`;
  };

  // Google sends the session back in the address
  if (location.hash.includes('access_token=')) {
    const got = new URLSearchParams(location.hash.slice(1));
    keep({ access_token: got.get('access_token'), refresh_token: got.get('refresh_token'), expires_in: Number(got.get('expires_in')) });
    history.replaceState(null, '', location.pathname + location.search);
  }

  const token = async () => {
    const session = saved();
    if (!session) return null;
    if (session.until - Date.now() > RENEW_MS) return session.access;
    try {
      keep(await auth('token?grant_type=refresh_token', { refresh_token: session.refresh }));
      return saved().access;
    } catch {
      return null;
    }
  };
  const api = async (path, method = 'GET', body) => {
    const access = await token();
    if (!access) return leave();
    const answer = await fetch(API + path, {
      method,
      headers: { Authorization: `Bearer ${access}`, ...(body && { 'Content-Type': 'application/json' }) },
      body: body && JSON.stringify(body),
    });
    if (answer.status === 401) return leave();
    if (!answer.ok) throw new Error(`${method} ${path}: ${answer.status}`);
    return answer.status === 204 ? null : answer.json();
  };
  // A list is read page by page to its end
  const all = async path => {
    const items = [];
    let cursor = '';
    do {
      const page = await api(`${path}${path.includes('?') ? '&' : '?'}limit=100${cursor}`);
      items.push(...page.items);
      cursor = page.next ? `&cursor=${encodeURIComponent(page.next)}` : '';
    } while (cursor);
    return items;
  };

  window.live = { api, auth, keep, leave, signOut, google, signedIn: () => Boolean(saved()) };

  // The page scripts were written over three lists; they are filled here and the scripts run after
  const then = document.currentScript.dataset.then;
  if (!then) return;
  const load = async () => {
    const [walkRows, photoRows, ...entries] = await Promise.all([
      all('/walks'), all('/photos'), ...['mood', 'weight', 'pressure', 'glucose'].map(kind => all(`/health?kind=${kind}`)),
    ]);
    window.walks = walkRows.map(walk => ({
      id: walk.id, number: walk.number, name: walk.name, start: walk.started_at, sec: walk.duration_s, steps: walk.steps,
      m: walk.distance_m, kcal: walk.calories, route: walk.route ? walk.route.coordinates : [], note: walk.note,
      mood: walk.mood_before && walk.mood_after ? [walk.mood_before, walk.mood_after] : null,
    }));
    window.finds = photoRows.map(find => ({
      id: find.id, walk: find.walk_id, at: find.taken_at, lon: find.longitude, lat: find.latitude, kind: find.kind,
      name: find.name, latin: find.latin_name, best: find.is_best, photo: find.url,
    }));
    window.health = entries.flat()
      .map(entry => ({ id: entry.id, kind: entry.kind, at: entry.measured_at, value: entry.value, low: entry.diastolic }))
      .sort((a, b) => b.at.localeCompare(a.at));
    // The page of one walk draws its whole route, the lists carry a simplified one
    const opened = new URLSearchParams(location.search).get('id');
    const walk = document.body.dataset.page === 'walk-page' && window.walks.find(item => item.id === opened);
    if (walk) {
      const whole = await api(`/walks/${opened}`);
      if (whole.route) walk.route = whole.route.coordinates;
    }
    for (const src of then.split(' ')) {
      await new Promise((done, failed) => {
        const script = Object.assign(document.createElement('script'), { src, onload: done, onerror: failed });
        document.body.append(script);
      });
    }
  };
  load().catch(problem => document.body.insertAdjacentHTML('beforeend', `<p class="empty">${problem.message}</p>`));
})();
