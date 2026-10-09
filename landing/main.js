const MOBILE = window.matchMedia('(max-width: 767px)');
const SVG_NS = 'http://www.w3.org/2000/svg';
const WAITLIST_URL = 'https://qangmeeugxafvlfcuxyp.supabase.co/functions/v1/waitlist';
const SEND_FAILED = "Couldn't save it. Please try again.";

// [kind, x, y, size, rotation] in design coordinates: 1440px desktop, 375px mobile
const DECOR = {
  desktop: [
    ['orange', 114, 1389, 10], ['leaf-violet', 299, 1151, 29, -168], ['blue', 1337, 1173, 6],
    ['blue', 181, 1661, 6], ['orange', 811, 1707, 9], ['leaf-violet', 1334, 1478, 18, -39],
    ['blue', 642, 1819, 7], ['orange', 843, 1855, 8], ['orange', 1267, 1862, 8],
    ['leaf-mint', 78, 2329, 26, 72], ['blue', 579, 2213, 7], ['blue', 889, 2212, 10],
    ['orange', 1246, 2196, 6], ['leaf-cyan', 308, 2803, 19, -2], ['blue', 621, 2711, 6],
    ['orange', 1400, 2695, 6], ['orange', 715, 3081, 7], ['leaf-green', 765, 2982, 30, 76],
    ['blue', 1282, 3065, 7], ['blue', 58, 3591, 8], ['orange', 529, 3595, 9],
    ['leaf-violet', 861, 3399, 21, -67], ['blue', 1316, 3322, 7], ['orange', 80, 3942, 10],
    ['orange', 596, 3982, 7], ['leaf-violet', 317, 4144, 26, 140], ['blue', 707, 4290, 6],
    ['blue', 1063, 4273, 6], ['orange', 1349, 4136, 9], ['leaf-mint', 701, 4504, 18, -73],
    ['blue', 935, 4560, 7], ['orange', 1359, 4607, 8],
  ],
  mobile: [
    ['orange', 330, 900, 10], ['leaf-violet', 33, 1000, 18, -39], ['blue', 300, 1130, 7],
    ['leaf-cyan', 340, 1600, 19, -2], ['orange', 36, 2080, 6], ['blue', 330, 2560, 6],
    ['leaf-violet', 30, 3071, 26, 140], ['orange', 320, 3590, 9], ['blue', 60, 3700, 7],
    ['leaf-violet', 323, 4880, 18, -39],
  ],
};

const LEAF_COLORS = {
  'leaf-violet': '#6c63ff',
  'leaf-cyan': '#13f1fc',
  'leaf-mint': '#8fd9a8',
  'leaf-green': '#57b894',
};

const FIREFLY_COLORS = {
  orange: ['#ffc83d', '#ffe680'],
  blue: ['#70ddff', '#13f1fc'],
};

const TRAIL = {
  desktop: { edge: 0, radius: 80, road: 96, firstGap: 40, lastGap: 100, step: 40, foot: 12, end: 160 },
  mobile: { edge: 12, radius: 32, road: 24, firstGap: 48, lastGap: 48, step: 22, foot: 6, end: 120 },
};

const page = document.querySelector('.page');
const trail = document.querySelector('.trail');
const decor = document.querySelector('.decor');

function initMenu() {
  const button = document.querySelector('.menu-button');
  const nav = document.querySelector('.nav');

  const toggle = (open) => {
    nav.classList.toggle('open', open);
    button.setAttribute('aria-expanded', String(open));
    button.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
    button.querySelector('use').setAttribute('href', `#${open ? 'close' : 'menu'}`);
  };

  button.addEventListener('click', () => toggle(!nav.classList.contains('open')));
  nav.addEventListener('click', (event) => {
    if (event.target.closest('a')) toggle(false);
  });
}

function initWaitlist() {
  const form = document.querySelector('.waitlist');
  const message = form.querySelector('.waitlist-message');
  const button = form.querySelector('.waitlist-button');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const email = form.email.value.trim();
    if (!form.email.checkValidity() || email === '') {
      message.textContent = 'Please enter a valid email.';
      return;
    }
    button.disabled = true;
    try {
      const reply = await fetch(WAITLIST_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        // People never see the website field; a script that fills it is not added to the list.
        body: JSON.stringify({ email, website: form.website.value }),
      });
      if (reply.ok) form.reset();
      message.textContent = reply.ok
        ? "Thanks! You're on the list."
        : reply.status === 400 ? 'Please enter a valid email.' : SEND_FAILED;
    } catch {
      message.textContent = SEND_FAILED;
    } finally {
      button.disabled = false;
    }
  });
}

// keeps decor at the same distance from the edges of the content it was designed around
function decorX(x, mobile) {
  const width = page.clientWidth;
  if (mobile) return x < 375 / 2 ? x : width - (375 - x);
  const frame = Math.min(width, 1440);
  return (width - frame) / 2 + x * frame / 1440;
}

function renderDecor() {
  const mobile = MOBILE.matches;

  decor.replaceChildren(...DECOR[mobile ? 'mobile' : 'desktop'].map(([kind, x, y, size, rotation = 0], i) => {
    const el = document.createElement('span');
    el.style.left = `${Math.round(decorX(x, mobile))}px`;
    el.style.top = `${y}px`;
    el.style.width = `${size}px`;
    el.style.height = `${size}px`;
    el.style.setProperty('--delay', `${-((i * 1.7) % 9).toFixed(1)}s`);

    if (kind.startsWith('leaf')) {
      el.className = 'leaf';
      el.style.height = 'auto';
      el.style.setProperty('--rotation', `${rotation}deg`);
      el.style.setProperty('--duration', `${8 + (i % 4) * 2}s`);
      el.innerHTML = `<svg viewBox="0 0 22 14"><path d="M1 13C2 5 9 0 21 1C19 9 12 14 1 13Z" fill="${LEAF_COLORS[kind]}"/><path d="M2 12C8 8 13 5 18 3" stroke="#fff" stroke-opacity=".6" fill="none"/></svg>`;
    } else {
      el.className = 'firefly';
      el.style.setProperty('--color', FIREFLY_COLORS[kind][0]);
      el.style.setProperty('--color-alt', FIREFLY_COLORS[kind][1]);
      el.style.setProperty('--duration', `${2 + (i % 3)}s`);
    }
    return el;
  }));
}

function offsetTop(el) {
  return el.getBoundingClientRect().top - page.getBoundingClientRect().top;
}

function buildTrailPath(cfg) {
  const width = page.clientWidth;
  const steps = document.querySelector('.steps').getBoundingClientRect();
  const stepsStyle = getComputedStyle(document.querySelector('.steps'));
  const pageLeft = page.getBoundingClientRect().left;
  const left = steps.left - pageLeft + parseFloat(stepsStyle.paddingLeft) - cfg.edge;
  const right = steps.right - pageLeft - parseFloat(stepsStyle.paddingRight) + cfg.edge;
  const r = cfg.radius;
  const showcase = document.querySelector('.showcase');
  const phone = document.querySelector('.app-phone');
  const rows = [...document.querySelectorAll('.step')].map((el) => [offsetTop(el), offsetTop(el) + el.offsetHeight]);

  const startX = width / 2;
  const startY = offsetTop(showcase) + showcase.offsetHeight / 2;
  const turnY = rows[0][0] - cfg.firstGap;
  let d = `M${startX} ${startY} L${startX} ${turnY - r} Q${startX} ${turnY} ${startX - r} ${turnY}`;
  d += ` L${left + r} ${turnY} Q${left} ${turnY} ${left} ${turnY + r}`;

  rows.forEach(([, bottom], i) => {
    const x = i % 2 === 0 ? left : right;
    const dir = i % 2 === 0 ? 1 : -1;
    const y = i < rows.length - 1 ? (bottom + rows[i + 1][0]) / 2 : bottom + cfg.lastGap;
    d += ` L${x} ${y - r} Q${x} ${y} ${x + dir * r} ${y}`;
    if (i < rows.length - 1) {
      const next = i % 2 === 0 ? right : left;
      d += ` L${next - dir * r} ${y} Q${next} ${y} ${next} ${y + r}`;
    } else {
      const phoneRect = phone.getBoundingClientRect();
      const phoneX = phoneRect.left + phoneRect.width / 2 - page.getBoundingClientRect().left;
      const phoneY = offsetTop(phone);
      const turn = phoneX > x ? 1 : -1;
      d += ` L${phoneX - turn * r} ${y} Q${phoneX} ${y} ${phoneX} ${y + r} L${phoneX} ${phoneY + cfg.end}`;
    }
  });
  return d;
}

function renderTrail() {
  const cfg = TRAIL[MOBILE.matches ? 'mobile' : 'desktop'];
  const svg = document.createElementNS(SVG_NS, 'svg');
  const path = document.createElementNS(SVG_NS, 'path');
  svg.setAttribute('width', page.clientWidth);
  svg.setAttribute('height', page.scrollHeight);
  path.setAttribute('d', buildTrailPath(cfg));
  path.setAttribute('fill', 'none');
  path.setAttribute('stroke', '#fcfcfd');
  path.setAttribute('stroke-width', cfg.road);
  path.setAttribute('stroke-linecap', 'round');
  path.setAttribute('stroke-linejoin', 'round');
  svg.append(path);
  trail.replaceChildren(svg);

  const length = path.getTotalLength();
  const prints = [];
  for (let s = cfg.step / 2, k = 0; s < length; s += cfg.step, k++) {
    const p = path.getPointAtLength(s);
    const q = path.getPointAtLength(Math.min(length, s + 1));
    const angle = Math.atan2(q.y - p.y, q.x - p.x);
    const side = (k % 2 ? 1 : -1) * cfg.foot * 0.6;
    const x = p.x - Math.sin(angle) * side;
    const y = p.y + Math.cos(angle) * side;

    const foot = document.createElement('span');
    foot.className = 'footprint';
    foot.style.setProperty('--size', `${cfg.foot}px`);
    foot.style.left = `${x - cfg.foot / 2}px`;
    foot.style.top = `${y - cfg.foot}px`;
    // the print is drawn toes up, so rotate it to face the direction of travel
    foot.style.transform = `rotate(${angle * 180 / Math.PI + 90}deg)`;
    foot.innerHTML = '<svg viewBox="0 0 14 28"><path d="M7 0C11 0 13 4 13 9C13 14 10 17 7 17C4 17 1 14 1 9C1 4 3 0 7 0Z" fill="#f1f2f4"/><ellipse cx="7" cy="23.5" rx="5" ry="4.5" fill="#f1f2f4"/></svg>';
    trail.append(foot);
    prints.push([y, foot]);
  }
  revealFootprints.prints = prints;
  revealFootprints();
}

function revealFootprints() {
  const limit = window.scrollY + window.innerHeight * 0.8 - (page.getBoundingClientRect().top + window.scrollY);
  for (const [y, foot] of revealFootprints.prints || []) {
    foot.classList.toggle('visible', y < limit);
  }
}

function initHeader() {
  const header = document.querySelector('.header');
  const nav = document.querySelector('.nav');
  let last = window.scrollY;

  window.addEventListener('scroll', () => {
    const y = window.scrollY;
    header.classList.toggle('scrolled', y > 40);
    // a few pixels of drift come from images loading, not from the reader
    if (Math.abs(y - last) < 8) return;
    header.classList.toggle('hidden', y > last && y > 200 && !nav.classList.contains('open'));
    last = y;
  }, { passive: true });
  window.addEventListener('mousemove', (event) => {
    if (event.clientY < 72) header.classList.remove('hidden');
  });
}

function initActiveNav() {
  const links = [...document.querySelectorAll('.nav a')];
  const visible = new Set();
  const observer = new IntersectionObserver((entries) => {
    for (const entry of entries) visible[entry.isIntersecting ? 'add' : 'delete'](entry.target.id);
    for (const link of links) link.classList.toggle('active', visible.has(link.hash.slice(1)));
  }, { rootMargin: '-45% 0px -45% 0px' });
  for (const link of links) observer.observe(document.querySelector(link.hash));
}

function render() {
  renderDecor();
  renderTrail();
}

initMenu();
initHeader();
initWaitlist();
initActiveNav();
window.addEventListener('load', render);
window.addEventListener('scroll', revealFootprints, { passive: true });

let resizeTimer;
window.addEventListener('resize', () => {
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(render, 150);
});
