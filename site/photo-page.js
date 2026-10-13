const shown = finds.find(item => item.id === param('id')) || finds[0];
const walk = walks.find(item => item.id === shown.walk);
const same = shown.name ? finds.filter(item => item.name === shown.name) : [shown];
const title = document.querySelector('#name');
const set = (id, text) => {
  const el = document.querySelector(id);
  el.textContent = text || '';
  el.hidden = !text;
};

document.title = `${findName(shown)} — WalkPick`;
document.querySelector('#photo').innerHTML = findPicture(shown);
title.textContent = findName(shown);
set('#kind', t(...kinds[shown.kind]));
set('#latin', shown.latin);
set('#text', shown.text || (shown.name ? '' : t('WalkPick could not name this one. Give it a name yourself.', 'WalkPick не смог это назвать. Дайте название сами.')));
set('#match', shown.match && t(`WalkPick is ${shown.match}% sure about the name.`, `WalkPick уверен в названии на ${shown.match}%.`));
set('#seen', same.length > 1 ? t(`Met ${same.length} times`, `Встречено раз: ${same.length}`) : t('Met once', 'Встречено один раз'));
set('#taken', `${day(shown.at, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })} · ${clock(shown.at)}`);

const wiki = document.querySelector('#wiki');
wiki.hidden = !shown.wiki;
wiki.href = shown.wiki || '';
const walkLink = document.querySelector('#walk-link');
walkLink.href = `walk-page.html?id=${walk.id}`;
walkLink.innerHTML = `${t('From the walk', 'С прогулки')} «${walkName(walk)}» ${icon('arrow')}`;

document.querySelector('#rename').addEventListener('click', () => {
  title.focus();
  getSelection().selectAllChildren(title);
});
const best = document.querySelector('#best');
best.setAttribute('aria-pressed', shown.best);
best.addEventListener('click', () => best.setAttribute('aria-pressed', best.getAttribute('aria-pressed') !== 'true'));

// Months of the year in which the same name was photographed, over all years
const seenMonths = new Set(same.map(item => new Date(item.at).getMonth()));
document.querySelector('#months').innerHTML = Array.from({ length: 12 }, (_, month) =>
  `<span${seenMonths.has(month) ? ' data-seen' : ''}>${day(new Date(2026, month, 1), { month: 'short' })}</span>`).join('');

walkMap(document.querySelector('#map'), walk, [shown], 64);
