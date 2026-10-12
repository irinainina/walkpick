const last = walks[0];
const lastPhotos = photosOf(last);
walkMap(document.querySelector('#map'), last, lastPhotos);
document.querySelector('#last-walk').innerHTML = `
  <p class="eyebrow">${t('Last walk', 'Последняя прогулка')}</p>
  <div>
    <h2>${walkName(last)}</h2>
    <p>${day(last.start)} · ${clock(last.start)}</p>
  </div>
  ${last.mood ? moodPair(last.mood) : ''}
  <dl class="stats">${walkStats(last)}</dl>
  ${lastPhotos.length ? `<div class="thumbs">${lastPhotos.slice(0, 4).map(findPicture).join('')}</div>` : ''}
  <a class="link" href="walk-page.html?id=${last.id}">${t('Open the walk', 'Открыть прогулку')}${lastPhotos.length ? ` · ${lastPhotos.length} ${t('photos', 'фото')}` : ''} ${icon('arrow')}</a>`;

const walkRow = walk => {
  const count = finds.filter(find => find.walk === walk.id).length;
  return `<a class="walk-row" href="walk-page.html?id=${walk.id}">
    ${routeSvg(walk)}
    <span><strong>${walkName(walk)}</strong><small>${clock(walk.start)}${count ? ` · ${count} ${t('photos', 'фото')}` : ''}</small></span>
    <span>${km(walk.m)}</span><span>${duration(walk.sec)}</span><span>${number(walk.steps)} ${t('steps', 'шагов')}</span>
    ${walk.mood ? moodPair(walk.mood) : `<span>${pace(walk)} ${t('min/km', 'мин/км')}</span>`}
  </a>`;
};
const walkList = list => [...new Set(list.map(walk => dayKey(walk.start)))].map(date => {
  const ofDay = list.filter(walk => dayKey(walk.start) === date);
  const total = ofDay.length > 1 ? `<span>${ofDay.length} ${t('walks', 'прогулки')} · ${km(sum(ofDay, 'm'))} · ${duration(sum(ofDay, 'sec'))}</span>` : '';
  return `<p class="day-title">${day(ofDay[0].start, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })}${total}</p>${ofDay.map(walkRow).join('')}`;
}).join('');

const titles = { week: t('This week', 'Эта неделя'), month: day(new Date(), { month: 'long', year: 'numeric' }), year: String(new Date().getFullYear()), all: t('All walks', 'Все прогулки') };
const showPeriod = period => {
  const inside = inPeriod(walks, period, 'start');
  document.querySelector('#period-title').textContent = titles[period];
  document.querySelector('#totals').innerHTML = stat(inside.length, t('walks', 'прогулок')) + stat(number(sum(inside, 'm') / 1000, 1), t('km', 'км'))
    + stat(duration(sum(inside, 'sec')), t('on foot', 'в пути')) + stat(number(sum(inside, 'steps')), t('steps', 'шагов'));
  const bars = buckets(period, walks[walks.length - 1].start).map(bar => ({ ...bar, metres: sum(within(inside, 'start', bar), 'm') }));
  const top = Math.max(...bars.map(bar => bar.metres), 1);
  document.querySelector('#bars').innerHTML = bars.map(bar => `<div title="${bar.title}: ${km(bar.metres)}"><i style="height:${bar.metres / top * 100}%"${bar.metres ? ' data-walked' : ''}></i>${bar.label}</div>`).join('');
  document.querySelector('#bars').classList.toggle('bars-dense', bars.length > 14);
  paged(document.querySelector('#list'), inside, list => walkList(list) || `<p class="empty">${t('No walks in this period yet.', 'В этом периоде прогулок пока нет.')}</p>`);
};
const opened = Object.keys(periodNames).find(period => inPeriod(walks, period, 'start').length) || 'week';
periodSwitch(document.querySelector('#period'), opened, showPeriod);
showPeriod(opened);

// The last 53 weeks, a column per week from Monday; the darker the square, the longer the walks of that day
const grid = document.querySelector('.year-grid');
const perDay = {};
walks.forEach(walk => { perDay[dayKey(walk.start)] = (perDay[dayKey(walk.start)] || 0) + walk.m; });
const cell = periodStart('week');
cell.setDate(cell.getDate() - 52 * 7);
for (let index = 0; index < 53 * 7; index++) {
  const metres = perDay[dayKey(cell)] || 0;
  grid.insertAdjacentHTML('beforeend', `<i${metres ? ` data-level="${Math.min(4, Math.ceil(metres / 1500))}"` : ''} title="${day(cell)}${metres ? `: ${km(metres)}` : ''}"></i>`);
  cell.setDate(cell.getDate() + 1);
}
const yearWalks = walks.filter(walk => new Date(walk.start) > new Date(Date.now() - 371 * 864e5));
document.querySelector('#today').textContent = day(new Date(), { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
document.querySelector('#year-note').textContent = t(`${yearWalks.length} walks · ${km(sum(yearWalks, 'm'))} in the last twelve months. The darker the square, the longer the walk.`,
  `${yearWalks.length} прогулок · ${km(sum(yearWalks, 'm'))} за последние двенадцать месяцев. Чем темнее квадрат, тем длиннее прогулка.`);
