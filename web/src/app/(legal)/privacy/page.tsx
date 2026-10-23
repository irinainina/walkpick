import type { Metadata } from "next";
import Legal from "../legal";

export const metadata: Metadata = {
  title: "Privacy Policy — WalkPick",
  description: "What WalkPick keeps about you, where it is stored and how to delete it.",
};

const MAIL = <a href="mailto:irina.inina.rss@gmail.com">irina.inina.rss@gmail.com</a>;

const en = (
  <>
    <p>WalkPick is an Android app that records your walks and the plants, birds and places you photograph on the way. It is made and run by Irina Inina, an individual developer based in Ukraine. This page says what the app keeps about you, where it goes and how to remove it.</p>

    <h2>What WalkPick keeps</h2>
    <ul>
      <li><strong>Account.</strong> Your email address. If you sign in with Google, also your first name as Google gives it.</li>
      <li><strong>Profile.</strong> Sex, date of birth, height and target values, if you enter them. They are used to count calories, stride length and a healthy weight range.</li>
      <li><strong>Walks.</strong> Start and end time, duration, steps, distance, calories, the route as a line of GPS points, the name of the walk and your note.</li>
      <li><strong>Photos and finds.</strong> The photos you take in the app, where and when each was taken, and the name the app or you gave it.</li>
      <li><strong>Health entries.</strong> Mood and weight that you type in yourself.</li>
      <li><strong>Settings.</strong> Units, reminder time and days.</li>
    </ul>
    <p>WalkPick has no advertising and no analytics or tracking libraries. Your data is not sold and not shared for marketing.</p>

    <h2>Location, steps and camera</h2>
    <ul>
      <li><strong>Location</strong> is read only while you record a walk, take a photo or look at the map. A walk keeps recording with the screen off; a notification is shown the whole time. Outside a walk the app does not follow you.</li>
      <li><strong>Steps</strong> are read from the phone&apos;s step sensor between Start and Stop of a walk only.</li>
      <li><strong>Camera</strong> is used only when you open it in the app. WalkPick does not read your phone&apos;s gallery.</li>
    </ul>

    <h2>Where it is stored</h2>
    <p>Everything is kept on your phone and copied to your account on Supabase, a hosting service, on servers in the European Union (Ireland). Photos lie in private storage that only your account can read. The copy lets you sign in on another phone or on this website and get your history back.</p>

    <h2>Services that receive some data</h2>
    <ul>
      <li><strong>Supabase</strong> — stores your account and data and sends the sign-in code to your email.</li>
      <li><strong>Google</strong> — signs you in if you choose &quot;Continue with Google&quot;, and draws the map (Google Maps). Google receives the map areas you look at.</li>
      <li><strong>Pl@ntNet</strong> — a photo you mark as a plant is sent there to be identified. It goes without your name, email or location.</li>
      <li><strong>Wikipedia</strong> — is asked for articles about places near a point on the map and for descriptions of finds. The request carries coordinates or a name, not your account.</li>
      <li><strong>Open-Meteo</strong> — is asked for the weather at your location to show it on the Home screen and to name a walk. The request carries coordinates only.</li>
    </ul>
    <p>Birds are identified on the phone itself; those photos are not sent to an identification service.</p>

    <h2>How long it is kept and how to delete it</h2>
    <p>Your data is kept until you delete it. You can delete a single walk, photo or health entry in the app. <strong>Profile → Delete account</strong> removes your account, all your data and all your photos from the server for good. You can also ask for deletion by email.</p>

    <h2>Your rights</h2>
    <p>You can ask for a copy of your data, for a correction or for deletion, and you can withdraw the permissions for location, physical activity, camera and notifications in the phone&apos;s settings at any time. Write to the address below; you will get an answer within 30 days.</p>

    <h2>Health information</h2>
    <p>WalkPick is not a medical device. The numbers and remarks it shows are for your own reference and are not medical advice.</p>

    <h2>Children</h2>
    <p>WalkPick is not meant for children under 16 and does not knowingly keep their data.</p>

    <h2>Changes</h2>
    <p>If this policy changes, the new text is published on this page with a new date.</p>

    <h2>Contact</h2>
    <p>Irina Inina, {MAIL}</p>
  </>
);

const ru = (
  <>
    <p>WalkPick — приложение для Android, которое записывает ваши прогулки и растения, птиц и места, которые вы снимаете по дороге. Его делает и поддерживает Ирина Инина, частный разработчик из Украины. Здесь написано, что приложение хранит о вас, куда это уходит и как это удалить.</p>

    <h2>Что хранит WalkPick</h2>
    <ul>
      <li><strong>Аккаунт.</strong> Ваш адрес почты. Если вы входите через Google — ещё имя, которое передаёт Google.</li>
      <li><strong>Профиль.</strong> Пол, дата рождения, рост и цели, если вы их укажете. По ним считаются калории, длина шага и здоровый диапазон веса.</li>
      <li><strong>Прогулки.</strong> Время начала и конца, длительность, шаги, расстояние, калории, маршрут в виде линии из точек GPS, название прогулки и ваша заметка.</li>
      <li><strong>Фото и находки.</strong> Снимки, сделанные в приложении, где и когда сделан каждый и название, которое дали приложение или вы.</li>
      <li><strong>Записи здоровья.</strong> Настроение и вес, которые вы вводите сами.</li>
      <li><strong>Настройки.</strong> Единицы, время и дни напоминаний.</li>
    </ul>
    <p>В WalkPick нет рекламы, аналитики и библиотек слежения. Ваши данные не продаются и не передаются для маркетинга.</p>

    <h2>Местоположение, шаги и камера</h2>
    <ul>
      <li><strong>Местоположение</strong> читается только пока вы записываете прогулку, снимаете или смотрите карту. Прогулка пишется и с выключенным экраном; всё это время видно уведомление. Вне прогулки приложение за вами не следит.</li>
      <li><strong>Шаги</strong> берутся с датчика шагов телефона только между Start и Stop прогулки.</li>
      <li><strong>Камера</strong> работает, только когда вы открываете её в приложении. Галерею телефона WalkPick не читает.</li>
    </ul>

    <h2>Где это хранится</h2>
    <p>Всё хранится на вашем телефоне и копируется в ваш аккаунт на Supabase — хостинге с серверами в Европейском союзе (Ирландия). Фото лежат в закрытом хранилище, которое может читать только ваш аккаунт. Копия позволяет войти с другого телефона или на этом сайте и вернуть свою историю.</p>

    <h2>Сервисы, которые получают часть данных</h2>
    <ul>
      <li><strong>Supabase</strong> — хранит аккаунт и данные и присылает код входа на почту.</li>
      <li><strong>Google</strong> — выполняет вход, если вы выбрали «Continue with Google», и рисует карту (Google Maps). Google получает участки карты, которые вы смотрите.</li>
      <li><strong>Pl@ntNet</strong> — получает снимок, отмеченный как растение, чтобы его распознать. Снимок уходит без имени, почты и координат.</li>
      <li><strong>Wikipedia</strong> — получает запросы статей о местах рядом с точкой на карте и описаний находок. В запросе координаты или название, но не ваш аккаунт.</li>
      <li><strong>Open-Meteo</strong> — получает запрос погоды в вашем месте, чтобы показать её на экране Home и назвать прогулку. В запросе только координаты.</li>
    </ul>
    <p>Птицы распознаются на самом телефоне; эти снимки в сервисы распознавания не отправляются.</p>

    <h2>Как долго это хранится и как удалить</h2>
    <p>Данные хранятся, пока вы их не удалите. Отдельную прогулку, фото или запись здоровья можно удалить в приложении. <strong>Profile → Delete account</strong> навсегда удаляет с сервера аккаунт, все данные и все фото. Удаление можно попросить и по почте.</p>

    <h2>Ваши права</h2>
    <p>Вы можете попросить копию своих данных, исправление или удаление и в любой момент отозвать в настройках телефона разрешения на местоположение, физическую активность, камеру и уведомления. Пишите на адрес ниже — ответ придёт в течение 30 дней.</p>

    <h2>Сведения о здоровье</h2>
    <p>WalkPick не медицинское изделие. Числа и замечания в нём — для вашего сведения, а не медицинский совет.</p>

    <h2>Дети</h2>
    <p>WalkPick не предназначен для детей младше 16 лет и сознательно не хранит их данные.</p>

    <h2>Изменения</h2>
    <p>Если политика изменится, новый текст появится на этой странице с новой датой.</p>

    <h2>Контакты</h2>
    <p>Ирина Инина, {MAIL}</p>
  </>
);

export default function Privacy() {
  return <Legal en={{ title: "Privacy Policy", body: en }} ru={{ title: "Политика конфиденциальности", body: ru }} />;
}
