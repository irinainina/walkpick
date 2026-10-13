const walk = walks.find(item => item.id === param('id')) || walks[0];
const photos = photosOf(walk);
document.title = `${walkName(walk)} — WalkPick`;
document.querySelector('#name').textContent = walkName(walk);
document.querySelector('#when').textContent = `${day(walk.start, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })} · ${clock(walk.start)}`;
document.querySelector('#rename').addEventListener('click', () => document.querySelector('#name').focus());
document.querySelector('#stats').innerHTML = walkStats(walk) + stat(number(walk.kcal), t('kcal', 'ккал'));
document.querySelector('#note').value = walk.note || '';
document.querySelector('#mood').innerHTML = walk.mood ? walk.mood.map((value, index) => `<div>
  <svg><use href="#mood-${moods[value - 1][3]}"/></svg>
  <span>${index ? t('After the walk', 'После прогулки') : t('Before the walk', 'До прогулки')}<strong>${moodName(value)}</strong></span>
</div>`).join('') : `<p>${t('The mood was not asked on this walk.', 'На этой прогулке настроение не спрашивалось.')}</p>`;

walkMap(document.querySelector('#map'), walk, photos, 96);
const ribbon = document.querySelector('#ribbon');
ribbon.innerHTML = photos.map(find => findTile(find, clock(find.at))).join('');
if (!photos.length) document.querySelector('#ribbon-hint').textContent = t('No photos on this walk.', 'На этой прогулке фото нет.');

const highlight = (id, on) => {
  document.querySelectorAll(`[data-find="${id}"]`).forEach(el => el.classList.toggle(el.matches('.pin') ? 'pin-active' : 'tile-active', on));
};
[ribbon, document.querySelector('#map')].forEach(area => {
  area.addEventListener('mouseover', event => event.target.closest('[data-find]') && highlight(event.target.closest('[data-find]').dataset.find, true));
  area.addEventListener('mouseout', event => event.target.closest('[data-find]') && highlight(event.target.closest('[data-find]').dataset.find, false));
});
