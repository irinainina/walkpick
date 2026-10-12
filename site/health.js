const metrics = {
  mood: { name: ['Mood', 'Настроение'], unit: ['of 5', 'из 5'], digits: 1 },
  weight: { name: ['Weight', 'Вес'], unit: ['kg', 'кг'], digits: 1 },
  pressure: { name: ['Blood pressure', 'Давление'], unit: ['mmHg', 'мм рт. ст.'], digits: 0 },
  glucose: { name: ['Glucose', 'Глюкоза'], unit: ['mmol/L', 'ммоль/л'], digits: 1 },
};
const view = { metric: 'weight', period: 'year' };
const entriesOf = kind => health.filter(entry => entry.kind === kind);
const reading = (entry, digits) => (entry.kind === 'mood' ? moodName(entry.value) : [entry.value, entry.low].filter(Boolean).map(value => number(value, digits)).join('/'));
const walkedOn = date => {
  const metres = sum(walks.filter(walk => dayKey(walk.start) === dayKey(date)), 'm');
  return metres ? `${km(metres)} ${t('walked that day', 'пройдено в этот день')}` : t('No walk that day', 'В этот день без прогулки');
};
const mean = values => values.reduce((total, value) => total + value, 0) / values.length;

const drawChart = (metric, entries, bars, monthly) => {
  const [width, height, left, bottom] = [640, 320, 48, 40];
  const [from, to] = [bars[0].from.getTime(), bars[bars.length - 1].to.getTime()];
  const x = time => left + (time - from) / (to - from) * (width - left);
  // A year and longer would be a saw of single entries, so every month is drawn as its average
  const points = monthly
    ? bars.map(bar => [within(entries, 'at', bar), (bar.from.getTime() + bar.to.getTime()) / 2]).filter(([inside]) => inside.length)
      .map(([inside, time]) => ({ time, value: mean(inside.map(entry => entry.value)), low: inside[0].low && mean(inside.map(entry => entry.low)) }))
    : entries.map(entry => ({ time: new Date(entry.at).getTime(), value: entry.value, low: entry.low })).reverse();
  const values = points.flatMap(point => [point.value, point.low].filter(Boolean));
  const spread = Math.max(...values) - Math.min(...values) || 1;
  const [low, high] = [Math.floor(Math.min(...values) - spread * 0.2), Math.ceil(Math.max(...values) + spread * 0.1)];
  const y = value => (height - bottom) * (1 - (value - low) / (high - low)) + 8;
  const top = Math.max(...bars.map(bar => bar.metres), 1);
  const each = Math.ceil(bars.length / 10);
  const columns = bars.map((bar, index) => {
    const size = bar.metres / top * (height - bottom) * 0.5;
    const [start, end] = [x(bar.from.getTime()), x(bar.to.getTime())];
    return `<rect x="${(start + (end - start) * 0.2).toFixed(1)}" y="${(height - bottom - size).toFixed(1)}" width="${((end - start) * 0.6).toFixed(1)}" height="${size.toFixed(1)}" rx="4" class="chart-bar"><title>${bar.title}: ${km(bar.metres)}</title></rect>`
      + (index % each ? '' : `<text x="${((start + end) / 2).toFixed(1)}" y="${height - 12}" text-anchor="middle">${bar.label}</text>`);
  }).join('');
  const ticks = values.length ? [low, (low + high) / 2, high].map(value => `<line x1="${left}" x2="${width}" y1="${y(value)}" y2="${y(value)}" class="chart-grid"/><text x="${left - 12}" y="${y(value) + 5}" text-anchor="end">${number(value, value % 1 ? 1 : 0)}</text>`).join('') : '';
  const lines = ['value', 'low'].filter(key => points.some(point => point[key])).map((key, order) => {
    const path = points.map(point => `${x(point.time).toFixed(1)},${y(point[key]).toFixed(1)}`).join(' ');
    const dots = points.length <= 31 ? points.map(point => `<circle cx="${x(point.time).toFixed(1)}" cy="${y(point[key]).toFixed(1)}" r="4" class="chart-dot" data-order="${order}"/>`).join('') : '';
    return `<polyline points="${path}" class="chart-line" data-order="${order}"/>${dots}`;
  }).join('');
  document.querySelector('#chart').innerHTML = `<svg class="chart" viewBox="0 0 ${width} ${height}" role="img" aria-label="${t(...metric.name)}">${ticks}${columns}${lines}</svg>`;
};

const showMetric = () => {
  const metric = metrics[view.metric];
  const [name, unit] = [t(...metric.name), t(...metric.unit)];
  const all = entriesOf(view.metric);
  const entries = inPeriod(all, view.period, 'at');
  const monthly = view.period === 'year' || view.period === 'all';
  const bars = buckets(view.period, walks[walks.length - 1].start).map(bar => ({ ...bar, metres: sum(within(walks, 'start', bar), 'm') }));
  drawChart(metric, entries, bars, monthly);
  document.querySelector('#chart-title').textContent = t(`${name} and walking`, `${name} и прогулки`);
  document.querySelector('#legend').innerHTML = `<i class="legend-line"></i>${name}, ${unit}${monthly ? t(', monthly average', ', среднее за месяц') : ''}
    <i class="legend-bar"></i>${monthly ? t('Kilometres walked in a month', 'Километры за месяц') : t('Kilometres walked in a day', 'Километры за день')}`;
  const [latest, first] = [all[0], entries[entries.length - 1]];
  document.querySelector('#latest-label').textContent = latest ? `${t('Latest', 'Последняя запись')} · ${day(latest.at, { month: 'long', day: 'numeric' })}` : t('No entries yet', 'Записей пока нет');
  document.querySelector('#latest').innerHTML = latest ? `${reading(latest, metric.digits)}${view.metric === 'mood' ? '' : ` <small>${unit}</small>`}` : '—';
  const delta = entries.length > 1 ? entries[0].value - first.value : 0;
  document.querySelector('#change').textContent = entries.length > 1
    ? `${delta > 0 ? '+' : '−'}${number(Math.abs(delta), metric.digits)} ${unit} ${t('over the period', 'за период')} · ${entries.length} ${t('entries', 'записей')}`
    : t('Too few entries in this period to compare.', 'В этом периоде слишком мало записей для сравнения.');
  document.querySelector('#value-label').textContent = `${name}, ${unit}`;
  document.querySelector('#value').placeholder = latest ? reading(latest, metric.digits) : '';
  document.querySelector('#entry').hidden = view.metric === 'mood';
  document.querySelector('#mood-note').hidden = view.metric !== 'mood';
  paged(document.querySelector('#entries'), entries, list => list.map(entry => `<div class="entry-row">
    <span>${day(entry.at, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })} · ${clock(entry.at)}</span>
    <strong>${reading(entry, metric.digits)}${view.metric === 'mood' ? '' : ` ${unit}`}</strong>
    <span>${walkedOn(entry.at)}</span>
    ${view.metric === 'mood' ? '' : `<button class="icon-button" type="button" aria-label="${t('Edit', 'Изменить')}">${icon('pencil')}</button><button class="icon-button" type="button" aria-label="${t('Delete', 'Удалить')}">${icon('trash')}</button>`}
  </div>`).join('') || `<p class="empty">${t('No entries in this period.', 'В этом периоде записей нет.')}</p>`, 20);
};

const chips = document.querySelector('#metrics');
chips.innerHTML = Object.entries(metrics).map(([id, metric]) => `<button type="button" data-metric="${id}" aria-pressed="${id === view.metric}">${t(...metric.name)} · ${entriesOf(id).length}</button>`).join('');
chips.addEventListener('click', event => {
  const button = event.target.closest('button');
  if (!button) return;
  chips.querySelectorAll('button').forEach(item => item.setAttribute('aria-pressed', item === button));
  view.metric = button.dataset.metric;
  showMetric();
});
periodSwitch(document.querySelector('#period'), view.period, period => {
  view.period = period;
  showMetric();
});
showMetric();
document.querySelector('#date').value = dayKey(new Date());
