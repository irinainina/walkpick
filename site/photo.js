const state = { mode: 'all', kind: 'all', period: 'all', search: '', selecting: false };
const content = document.querySelector('#content');
const mapBox = document.querySelector('#map');
const bar = document.querySelector('#select-bar');
let map = null;

const usedKinds = Object.keys(kinds).filter(kind => finds.some(find => find.kind === kind));
document.querySelector('#kinds').innerHTML = [['all', 'Everything', 'Всё'], ...usedKinds.map(kind => [kind, ...kinds[kind]])]
  .map(([id, en, ru]) => `<button type="button" data-kind="${id}" aria-pressed="${id === 'all'}">${t(en, ru)}</button>`).join('');

const visible = () => inPeriod(finds, state.period, 'at').filter(find => (state.mode !== 'best' || find.best)
  && (state.kind === 'all' || find.kind === state.kind)
  && findName(find).toLowerCase().includes(state.search));

const months = list => [...new Set(list.map(find => dayKey(find.at).slice(0, 7)))].map(month => {
  const ofMonth = list.filter(find => dayKey(find.at).startsWith(month));
  return `<p class="day-title">${day(ofMonth[0].at, { month: 'long', year: 'numeric' })}</p>
    <div class="tiles">${ofMonth.map(find => findTile(find, find.latin || day(find.at, { month: 'short', day: 'numeric' }))).join('')}</div>`;
}).join('');

const show = () => {
  const list = visible();
  const onMap = state.mode === 'map';
  const names = new Set(list.filter(find => find.name).map(find => find.name)).size;
  document.querySelector('#summary').textContent = t(`${list.length} photos · ${names} different names`, `${list.length} фото · ${names} разных названий`);
  content.hidden = onMap;
  mapBox.hidden = !onMap;
  if (map) map.remove();
  map = onMap && list.length ? findsMap(mapBox, list) : null;
  if (!onMap) paged(content, list, part => months(part) || `<p class="empty">${t('Nothing found. Try another name, kind or period.', 'Ничего не нашлось. Попробуйте другое название, вид или период.')}</p>`, 60);
};

const pick = (group, key) => group.addEventListener('click', event => {
  const button = event.target.closest('button');
  if (!button) return;
  group.querySelectorAll('button').forEach(item => item.setAttribute('aria-pressed', item === button));
  state[key] = button.dataset[key];
  show();
});
pick(document.querySelector('#mode'), 'mode');
pick(document.querySelector('#kinds'), 'kind');
periodSwitch(document.querySelector('#period'), state.period, period => {
  state.period = period;
  show();
});
document.querySelector('#search').addEventListener('input', event => {
  state.search = event.target.value.trim().toLowerCase();
  show();
});

const countSelected = () => {
  const count = content.querySelectorAll('[aria-selected="true"]').length;
  document.querySelector('#selected').textContent = t(`${count} selected`, `Выбрано: ${count}`);
};
document.querySelector('#select').addEventListener('click', event => {
  state.selecting = !state.selecting;
  event.target.textContent = state.selecting ? t('Done', 'Готово') : t('Select', 'Выбрать');
  content.classList.toggle('selecting', state.selecting);
  bar.hidden = !state.selecting;
  content.querySelectorAll('[aria-selected]').forEach(tile => tile.removeAttribute('aria-selected'));
  countSelected();
});
content.addEventListener('click', event => {
  const tile = event.target.closest('.tile');
  if (!state.selecting || !tile) return;
  event.preventDefault();
  tile.setAttribute('aria-selected', tile.getAttribute('aria-selected') !== 'true');
  countSelected();
});

show();
