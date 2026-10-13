let lang = /^(ru|uk|be)/.test(navigator.language) ? 'ru' : 'en';
try { lang = localStorage.getItem('lang') || lang; } catch {}

const page = document.body.dataset.page;
const t = (en, ru) => (lang === 'ru' ? ru : en);
const number = (value, digits = 0) => value.toLocaleString(lang === 'ru' ? 'ru-RU' : 'en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits });
const kinds = { plant: ['Plant', 'Растение'], bird: ['Bird', 'Птица'], place: ['Place', 'Место'], photo: ['Photo', 'Фото'], unknown: ['Unknown', 'Неизвестно'] };
// The five answers of the app from the worst to the best: two names, the colour and the icon
const moods = [['Awful', 'Ужасно', '#ff6b6b', 'awful'], ['Bad', 'Плохо', '#ff9f43', 'bad'], ['Neutral', 'Нормально', '#f5b82e', 'neutral'], ['Good', 'Хорошо', '#8fd16a', 'good'], ['Great', 'Отлично', '#2ecc8f', 'great']];
const locale = lang === 'ru' ? 'ru-RU' : 'en-US';
const km = metres => `${number(metres / 1000, 1)} ${t('km', 'км')}`;
const duration = seconds => {
  const min = Math.round(seconds / 60);
  return min < 60 ? `${min} ${t('min', 'мин')}` : `${Math.floor(min / 60)} ${t('h', 'ч')} ${min % 60} ${t('min', 'мин')}`;
};
const day = (date, options = { weekday: 'long', month: 'long', day: 'numeric' }) => new Date(date).toLocaleDateString(locale, options).replace(/^./, letter => letter.toUpperCase());
const clock = date => new Date(date).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' });
const dayKey = date => new Date(date).toLocaleDateString('sv-SE');
const param = name => new URLSearchParams(location.search).get(name);
const sum = (items, key) => items.reduce((total, item) => total + item[key], 0);

// Week, Month and Year are the current calendar ones, as in the app
const periodNames = { week: ['Week', 'Неделя'], month: ['Month', 'Месяц'], year: ['Year', 'Год'], all: ['All', 'Всё'] };
const periodStart = name => {
  const start = new Date();
  start.setHours(0, 0, 0, 0);
  if (name === 'week') start.setDate(start.getDate() - (start.getDay() + 6) % 7);
  if (name === 'month') start.setDate(1);
  if (name === 'year') start.setMonth(0, 1);
  return name === 'all' ? new Date(0) : start;
};
const inPeriod = (items, name, key) => items.filter(item => new Date(item[key]) >= periodStart(name));
const periodSwitch = (group, chosen, onChange) => {
  group.innerHTML = Object.entries(periodNames).map(([id, names]) => `<button type="button" data-period="${id}" aria-pressed="${id === chosen}">${t(...names)}</button>`).join('');
  group.addEventListener('click', event => event.target.dataset.period && onChange(event.target.dataset.period));
};
// The bars of a period: days for a week and a month, months for a year and for everything
const buckets = (period, first) => {
  const byMonth = period === 'year' || period === 'all';
  const cursor = period === 'all' ? new Date(first) : periodStart(period);
  cursor.setHours(0, 0, 0, 0);
  if (byMonth) cursor.setDate(1);
  const end = new Date(cursor);
  if (period === 'week') end.setDate(end.getDate() + 6);
  if (period === 'month') end.setMonth(end.getMonth() + 1, 0);
  if (period === 'year') end.setMonth(11, 31);
  if (period === 'all') end.setTime(Date.now());
  const list = [];
  while (cursor <= end) {
    const from = new Date(cursor);
    if (byMonth) cursor.setMonth(cursor.getMonth() + 1); else cursor.setDate(cursor.getDate() + 1);
    const label = period === 'week' ? day(from, { weekday: 'short' }) : period === 'month' ? from.getDate() : day(from, { month: 'narrow' });
    list.push({ from, to: new Date(cursor), label, title: byMonth ? day(from, { month: 'long', year: 'numeric' }) : day(from) });
  }
  return list;
};
const within = (items, key, bucket) => items.filter(item => new Date(item[key]) >= bucket.from && new Date(item[key]) < bucket.to);
// A long list opens by pages behind one button
const paged = (box, items, render, size = 30) => {
  let shown = size;
  const draw = () => {
    box.innerHTML = render(items.slice(0, shown)) + (shown < items.length ? `<button class="button button-ghost more" type="button">${t('Show more', 'Показать ещё')} · ${items.length - shown}</button>` : '');
    const more = box.querySelector('.more');
    if (more) more.addEventListener('click', () => { shown += size; draw(); });
  };
  draw();
};

const moodColor = value => moods[value - 1][2];
const moodName = value => t(...moods[value - 1]);
const moodPair = ([before, after]) => `<span class="mood"><i style="background:${moodColor(before)}"></i>${moodName(before)} → <i style="background:${moodColor(after)}"></i>${moodName(after)}</span>`;
const icon = name => `<svg class="icon"><use href="#${name}"/></svg>`;
const stat = (value, label) => `<div><dt>${label}</dt><dd>${value}</dd></div>`;
const walkName = walk => walk.name || t(`Walk ${walk.number}`, `Прогулка ${walk.number}`);
const pace = walk => {
  const perKm = walk.sec / (walk.m / 1000);
  return `${Math.floor(perKm / 60)}:${String(Math.floor(perKm % 60)).padStart(2, '0')}`;
};
const walkStats = walk => stat(number(walk.m / 1000, 1), t('km', 'км')) + stat(Math.round(walk.sec / 60), t('min', 'мин')) + stat(number(walk.steps), t('steps', 'шагов')) + stat(pace(walk), t('min/km', 'мин/км'));
const photosOf = walk => finds.filter(find => find.walk === walk.id).reverse();
const findName = find => find.name || t(...kinds[find.kind]);
const findPicture = find => (find.photo
  ? `<img src="assets/photos/${find.id}.jpg" alt="" loading="lazy">`
  : `<span class="no-photo" data-kind="${find.kind}">${icon(`kind-${find.kind}`)}</span>`);
const findTile = (find, caption) => `<a class="tile" href="photo-page.html?id=${find.id}" data-find="${find.id}">
  ${findPicture(find)}
  ${find.best ? icon('star') : ''}
  <span class="tile-text"><strong>${findName(find)}</strong>${caption}</span>
</a>`;

// A walk drawn as a small line: longitudes are squeezed by cos(latitude) to keep the shape true
const routeSvg = walk => {
  const squeeze = Math.cos(walk.route[0][1] * Math.PI / 180);
  const points = walk.route.map(([lon, lat]) => [lon * squeeze, -lat]);
  const xs = points.map(point => point[0]);
  const ys = points.map(point => point[1]);
  const size = Math.max(Math.max(...xs) - Math.min(...xs), Math.max(...ys) - Math.min(...ys)) || 1;
  const path = points.map(([x, y]) => `${((x - Math.min(...xs)) / size * 48 + 8).toFixed(1)},${((y - Math.min(...ys)) / size * 48 + 8).toFixed(1)}`).join(' ');
  return `<svg class="route-thumb" viewBox="0 0 64 64"><polyline points="${path}" fill="none" stroke="#2ecc8f" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
};

document.body.insertAdjacentHTML('afterbegin', `<svg class="sprite" aria-hidden="true">
  <symbol id="pin-lens" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M24 45s-15-12-15-24a15 15 0 0 1 30 0c0 12-15 24-15 24z"/><circle cx="24" cy="21" r="8.25"/><path fill="currentColor" fill-rule="evenodd" stroke="none" transform="translate(24 21.5) scale(.75)" d="M6.3 -6.4C1 -6.6 -4.5 -5 -6 -1C-6.6 0.6 -6.3 1.8 -5.6 2.6L-4.9 3.7C-3 5 0.5 5.2 2.2 3.4C4.2 1.3 3 -3 6.3 -6.4ZM-3.72 2.77L-3.29 2.01L-2.81 1.22L-2.27 0.42L-1.66 -0.38L-0.99 -1.17L-0.23 -1.94L0.61 -2.66L1.55 -3.34A.55 .55 0 0 0 0.95 -4.26L-0.07 -3.53L-0.98 -2.74L-1.8 -1.91L-2.52 -1.07L-3.17 -0.22L-3.73 0.63L-4.24 1.45L-4.68 2.23A.55 .55 0 0 0 -3.72 2.77Z"/></symbol>
  <symbol id="arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 17L17 7M9 7h8v8"/></symbol>
  <symbol id="back" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 5l-7 7 7 7"/></symbol>
  <symbol id="star" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3l2.7 5.6 6.1.8-4.5 4.3 1.1 6.1L12 16.9 6.6 19.8l1.1-6.1L3.200 9.400l6.100-.8z"/></symbol>
  <symbol id="pencil" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20l4-1L19 8l-3-3L5 16z"/></symbol>
  <symbol id="trash" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 7h14M10 7V4h4v3M7 7l1 13h8l1-13"/></symbol>
  <symbol id="search" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="6"/><path d="M16 16l4 4"/></symbol>
  <symbol id="kind-plant" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 19c0-8 5-13 14-14 0 9-5 14-13 14M5 19l7-7"/></symbol>
  <symbol id="kind-bird" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 15c1-6 6-9 11-7l5-2-2 4c1 5-3 9-8 9H5l4-4"/></symbol>
  <symbol id="kind-place" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20h16M6 20V9l6-4 6 4v11M10 20v-5h4v5"/></symbol>
  <symbol id="kind-photo" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 8h4l1-2h6l1 2h4v11H4z"/><circle cx="12" cy="13" r="3"/></symbol>
  <symbol id="kind-unknown" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M9.500 9.500a2.500 2.500 0 1 1 3.500 2.300c-.7.400-1 .9-1 1.700M12 17h.01"/></symbol>
  <symbol id="mail" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 6h16v12H4z M4 7l8 6 8-6"/></symbol>
  <symbol id="mood-great" viewBox="5 3 48 48" fill="none" stroke="currentColor" stroke-linecap="round" color="#2ecc8f"><path d="M29 48.5a21.5 21.5 0 1 0 0-43a21.5 21.5 0 0 0 0 43z" fill="#fff" stroke-width="3"/><path stroke="none" fill="currentColor" d="M22.5 25.6c1.436 0 2.6-1.612 2.6-3.6s-1.164-3.6-2.6-3.6-2.6 1.612-2.6 3.6 1.164 3.6 2.6 3.6zM35.5 25.6c1.436 0 2.6-1.612 2.6-3.6s-1.164-3.6-2.6-3.6-2.6 1.612-2.6 3.6 1.164 3.6 2.6 3.6z"/><path stroke-width="3" d="M21 33.5c5.333 5 10.667 5 16 0"/><path stroke-width="2.5" d="M16.4 28.6c-.8 1.333-.733 2.667.2 4M41.6 28.6c.8 1.333.733 2.667-.2 4"/></symbol>
  <symbol id="mood-good" viewBox="5 3 48 48" fill="none" stroke="currentColor" stroke-linecap="round" color="#8fd16a"><path d="M29 48.5a21.5 21.5 0 1 0 0-43a21.5 21.5 0 0 0 0 43z" fill="#fff" stroke-width="3"/><path stroke="none" fill="currentColor" d="M22.5 25.6c1.436 0 2.6-1.612 2.6-3.6s-1.164-3.6-2.6-3.6-2.6 1.612-2.6 3.6 1.164 3.6 2.6 3.6zM35.5 25.6c1.436 0 2.6-1.612 2.6-3.6s-1.164-3.6-2.6-3.6-2.6 1.612-2.6 3.6 1.164 3.6 2.6 3.6z"/><path stroke-width="3" d="M21 32c5.333 4.333 10.667 4.333 16 0"/></symbol>
  <symbol id="mood-neutral" viewBox="5 3 48 48" fill="none" stroke="currentColor" stroke-linecap="round" color="#f5b82e"><path d="M29 48.5a21.5 21.5 0 1 0 0-43a21.5 21.5 0 0 0 0 43z" fill="#fff" stroke-width="3"/><path stroke="none" fill="currentColor" d="M22.5 25.6c1.436 0 2.6-1.612 2.6-3.6s-1.164-3.6-2.6-3.6-2.6 1.612-2.6 3.6 1.164 3.6 2.6 3.6zM35.5 25.6c1.436 0 2.6-1.612 2.6-3.6s-1.164-3.6-2.6-3.6-2.6 1.612-2.6 3.6 1.164 3.6 2.6 3.6z"/><path stroke-width="3" d="M22 35.5l14-3"/></symbol>
  <symbol id="mood-bad" viewBox="5 3 48 48" fill="none" stroke="currentColor" stroke-linecap="round" color="#ff9f43"><path d="M29 48.5a21.5 21.5 0 1 0 0-43a21.5 21.5 0 0 0 0 43z" fill="#fff" stroke-width="3"/><path stroke="none" fill="currentColor" d="M22.5 25.6c1.436 0 2.6-1.612 2.6-3.6s-1.164-3.6-2.6-3.6-2.6 1.612-2.6 3.6 1.164 3.6 2.6 3.6zM35.5 25.6c1.436 0 2.6-1.612 2.6-3.6s-1.164-3.6-2.6-3.6-2.6 1.612-2.6 3.6 1.164 3.6 2.6 3.6z"/><path stroke-width="3" d="M21.5 37c5-4 10-4 15 0"/></symbol>
  <symbol id="mood-awful" viewBox="5 3 48 48" fill="none" stroke="currentColor" stroke-linecap="round" color="#ff6b6b"><path d="M29 48.5a21.5 21.5 0 1 0 0-43a21.5 21.5 0 0 0 0 43z" fill="#fff" stroke-width="3"/><path stroke-width="3" d="M19.5 24.5l6-4M32.5 20.5l6 4M20.5 38c5.667-6 11.333-6 17 0"/></symbol>
</svg>`);

const links = [['walk', 'Walk', 'Прогулка'], ['photo', 'Photo', 'Фото'], ['health', 'Health', 'Здоровье']];
const logo = '<a class="logo" href="index.html"><svg class="logo-mark"><use href="#pin-lens"/></svg>WalkPick</a>';
const header = document.querySelector('.header');
header.innerHTML = `<div class="header-inner glass">
  ${logo}
  ${page === 'sign-in' ? '' : `<nav class="nav">${links.map(([id, en, ru]) => `<a href="${id}.html" data-ru="${ru}"${page.startsWith(id) ? ' aria-current="page"' : ''}>${en}</a>`).join('')}</nav>`}
  <div class="lang" role="group" aria-label="Language">${['en', 'ru'].map(code => `<button type="button" data-lang="${code}">${code.toUpperCase()}</button>`).join('')}</div>
  ${page === 'home' ? '<a class="button button-small button-ghost" href="sign-in.html" data-ru="Войти">Sign in</a>' : ''}
  ${page === 'home' || page === 'sign-in' ? '' : `<a class="avatar" href="account.html" aria-label="${t('Account', 'Аккаунт')}">I</a>`}
</div>`;

const footer = document.querySelector('.footer');
if (footer) footer.innerHTML = `<div class="container footer-inner">
  ${logo}
  <ul class="footer-links">
    <li><a href="../landing/privacy.html" data-ru="Политика конфиденциальности">Privacy Policy</a></li>
    <li><a href="../landing/terms.html" data-ru="Условия использования">Terms of Use</a></li>
    <li>© 2026 WalkPick</li>
  </ul>
</div>`;

// The text written in the markup changes its language in place; a page drawn by scripts is loaded again
let onLanguage = null;
const swap = (attribute, ru, en, property) => document.querySelectorAll(`[${attribute}]`).forEach(el => {
  el.dataset[en] ??= el[property];
  el[property] = t(el.dataset[en], el.dataset[ru]);
});
const translate = () => {
  document.documentElement.lang = lang;
  document.querySelectorAll('[data-lang]').forEach(button => button.setAttribute('aria-pressed', button.dataset.lang === lang));
  swap('data-ru', 'ru', 'en', 'textContent');
  swap('data-ru-alt', 'ruAlt', 'enAlt', 'alt');
  swap('data-ru-placeholder', 'ruPlaceholder', 'enPlaceholder', 'placeholder');
};
translate();
document.querySelectorAll('[data-lang]').forEach(button => button.addEventListener('click', () => {
  lang = button.dataset.lang;
  try { localStorage.setItem('lang', lang); } catch {}
  if (!onLanguage) return location.reload();
  translate();
  onLanguage();
}));

document.querySelectorAll('.segmented:not(.lang)').forEach(group => group.addEventListener('click', event => {
  const pressed = event.target.closest('button');
  if (pressed) group.querySelectorAll('button').forEach(button => button.setAttribute('aria-pressed', button === pressed));
}));

let lastY = scrollY;
let shift = 0;
// The header moves with the page: it leaves and comes back exactly as far as the page was scrolled
const placeHeader = called => {
  const reach = header.offsetHeight + 16;
  if (footer && footer.getBoundingClientRect().top < reach + 32) shift = -reach;
  else shift = called ? 0 : Math.min(0, Math.max(-reach, shift - scrollY + lastY));
  lastY = scrollY;
  header.classList.toggle('header-called', called);
  header.style.transform = `translateY(${shift}px)`;
};
addEventListener('scroll', () => placeHeader(false));
addEventListener('mousemove', event => {
  if (event.clientY < 24 && shift < 0) placeHeader(true);
});
placeHeader(false);
