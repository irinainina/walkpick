const firstWalk = walks[walks.length - 1];
document.querySelector('#since').textContent = `${t('Walking since', 'Гуляет с')} ${day(firstWalk.start, { day: 'numeric', month: 'long', year: 'numeric' })}`;
document.querySelector('#totals').innerHTML = stat(number(walks.length), t('walks', 'прогулок')) + stat(number(sum(walks, 'm') / 1000), t('km', 'км'))
  + stat(number(finds.length), t('photos', 'фото')) + stat(number(new Set(finds.filter(find => find.name).map(find => find.name)).size), t('different names', 'разных названий'));
document.querySelector('#sign-out').addEventListener('click', live.signOut);
