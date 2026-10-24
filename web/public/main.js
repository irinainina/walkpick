const still = matchMedia('(prefers-reduced-motion: reduce)').matches;

// The counters go on from the middle of a walk: a second every second, about 45 steps a minute
const tally = { seconds: 34 * 60 + 48, steps: 3790 };
const showTally = () => {
  document.querySelector('#hud-time').textContent = `${String(Math.floor(tally.seconds / 60)).padStart(2, '0')}:${String(tally.seconds % 60).padStart(2, '0')}`;
  document.querySelector('#hud-km').textContent = number(tally.steps * 4 / 5230, 1);
  document.querySelector('#hud-steps').textContent = number(tally.steps);
};
if (!still) {
  setInterval(() => { tally.seconds++; showTally(); }, 1000);
  setInterval(() => { tally.steps++; showTally(); }, 1333);
}

const feeling = { before: 'neutral', after: '' };
const moodOf = id => moods.find(mood => mood[3] === id);
const moodBoxes = document.querySelectorAll('.moods');
moodBoxes.forEach(box => {
  box.innerHTML = moods.map(mood => `<button type="button" data-mood="${mood[3]}"><svg><use href="#mood-${mood[3]}"/></svg></button>`).join('') + '<span class="mood-answer"></span>';
  box.addEventListener('click', event => {
    const button = event.target.closest('button');
    if (!button) return;
    feeling[box.dataset.when] = button.dataset.mood;
    paint();
  });
});
const routeColor = share => `color-mix(in srgb, ${moodOf(feeling.after || feeling.before)[2]} ${Math.round(share * 100)}%, ${moodOf(feeling.before)[2]})`;

const route = document.querySelector('.route');
const line = route.querySelector('.route-line');
const [plan, walked] = line.querySelectorAll('path');
const you = line.querySelector('.route-you');
const dots = [...route.querySelectorAll('.stop-dot')];
let points = [];
let length = 0;

// The line runs down one side of a stop and crosses to the other side in the gap above the next one
const drawRoute = () => {
  const box = route.getBoundingClientRect();
  points = dots.map(dot => {
    const rect = dot.getBoundingClientRect();
    return [Math.round(rect.left - box.left + rect.width / 2), Math.round(rect.top - box.top + rect.height / 2)];
  });
  const path = points.slice(1).reduce((d, [x, y], index) => {
    const [fromX, fromY] = points[index];
    const turn = Math.min(400, y - fromY, Math.abs(x - fromX) / 2 + 80);
    return x === fromX ? `${d}V${y}` : `${d}V${y - turn}C${fromX},${y - turn / 2} ${x},${y - turn / 2} ${x},${y}`;
  }, `M${points[0]}`);
  plan.setAttribute('d', path);
  walked.setAttribute('d', path);
  line.querySelector('#mood').setAttribute('y2', box.height);
  length = walked.getTotalLength();
  walk();
};

// The walked part ends where the path meets the line at 60% of the window height
const walk = () => {
  const target = still ? Infinity : innerHeight * 0.6 - route.getBoundingClientRect().top;
  let from = 0;
  let to = length;
  for (let step = 0; step < 20; step++) {
    const middle = (from + to) / 2;
    if (walked.getPointAtLength(middle).y < target) from = middle; else to = middle;
  }
  const tip = walked.getPointAtLength(from);
  walked.style.strokeDasharray = `${from} ${length}`;
  you.setAttribute('cx', tip.x);
  you.setAttribute('cy', tip.y);
  const last = points[points.length - 1][1];
  dots.forEach((dot, index) => {
    dot.style.background = points[index][1] <= tip.y + 1 ? routeColor(points[index][1] / last) : '';
  });
};

let queued = false;
addEventListener('scroll', () => {
  if (queued) return;
  queued = true;
  requestAnimationFrame(() => { queued = false; walk(); });
});
// The route has the colour of the mood before the walk until the mood after it is chosen
const paint = () => {
  line.querySelectorAll('#mood stop').forEach((stop, index) => { stop.style.stopColor = routeColor(index); });
  moodBoxes.forEach(box => {
    const chosen = feeling[box.dataset.when];
    box.querySelectorAll('button').forEach(button => {
      button.setAttribute('aria-pressed', button.dataset.mood === chosen);
      button.setAttribute('aria-label', t(...moodOf(button.dataset.mood)));
    });
    box.querySelector('.mood-answer').textContent = chosen ? t(...moodOf(chosen)) : '';
  });
  if (length) walk();
};
new ResizeObserver(drawRoute).observe(route);

// The questions live in faq.json, so their text is edited without touching the page
const attr = text => text.replaceAll('"', '&quot;');
fetch('faq.json').then(answer => answer.json()).then(items => {
  document.querySelector('.faq').innerHTML = items.map(({ en, ru }, index) => `<details class="faq-item" name="faq"${index ? '' : ' open'}>
  <summary data-ru="${attr(ru.question)}">${en.question}</summary>
  <p data-ru="${attr(ru.answer)}">${en.answer}</p>
</details>`).join('');
  translate();
  const schema = document.createElement('script');
  schema.type = 'application/ld+json';
  schema.textContent = JSON.stringify({
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: items.map(item => ({ '@type': 'Question', name: item[lang].question, acceptedAnswer: { '@type': 'Answer', text: item[lang].answer } })),
  });
  document.head.append(schema);
});

// The language changes in place, so nothing that is moving on the page starts over
onLanguage = () => {
  showTally();
  paint();
};
onLanguage();
