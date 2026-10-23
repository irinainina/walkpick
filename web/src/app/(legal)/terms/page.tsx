import type { Metadata } from "next";
import Legal from "../legal";

export const metadata: Metadata = {
  title: "Terms of Use — WalkPick",
  description: "The terms of using WalkPick.",
};

const MAIL = <a href="mailto:irina.inina.rss@gmail.com">irina.inina.rss@gmail.com</a>;
const APACHE = <a href="https://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>;

const en = (
  <>
    <p>These terms are an agreement between you and Irina Inina, the developer of WalkPick. By creating an account or using the app you accept them. If you do not agree, please do not use the app.</p>

    <h2>The app</h2>
    <p>WalkPick records walks, counts steps, keeps the photos you take on the way and names the plants, birds and places in them. It is free and is offered as it is, without a promise that it will always be available or free of errors.</p>

    <h2>Your account</h2>
    <ul>
      <li>You need an email address or a Google account to sign in. Keep access to it: it is the only key to your data.</li>
      <li>You must be at least 16 years old.</li>
      <li>You can delete your account at any time in Profile.</li>
    </ul>

    <h2>Your content</h2>
    <p>Your walks, photos, notes and health entries remain yours. You let WalkPick store and process them only to show them to you and to identify what is in the photos, as the <a href="/privacy">Privacy Policy</a> describes. Take photos only where you are allowed to and respect other people&apos;s privacy.</p>

    <h2>Fair use</h2>
    <p>Do not use the app to break the law, do not try to reach other people&apos;s data, and do not load the service with automated requests. An account that does so may be closed.</p>

    <h2>Identification and health figures</h2>
    <ul>
      <li>The names of plants, birds and places are suggestions made by automatic services and can be wrong. Never decide whether a plant or a mushroom is safe to eat or touch from what the app says.</li>
      <li>Steps, distance, calories and health remarks are estimates. WalkPick is not a medical device and gives no medical advice; talk to a doctor about your health.</li>
      <li>Watch the road, not the screen. You walk at your own risk.</li>
    </ul>

    <h2>Other services</h2>
    <p>The app relies on Google Maps, Google Sign-In, Pl@ntNet, Wikipedia and Open-Meteo. Their content belongs to them and is shown under their own terms; descriptions from Wikipedia are available under the CC BY-SA licence. Birds are identified on the phone by Google&apos;s AIY Birds V1 model, used under the {APACHE}.</p>

    <h2>Liability</h2>
    <p>As far as the law allows, the developer is not liable for lost data, for wrong identifications or figures, or for any harm that follows from using the app. Nothing here limits the rights the law of your country gives you as a consumer.</p>

    <h2>Changes and the end of the service</h2>
    <p>The app and these terms may change; the new text is published on this page with a new date. If the service is ever closed, you will be told in advance so that you can save your data.</p>

    <h2>Law</h2>
    <p>These terms are governed by the law of Ukraine.</p>

    <h2>Contact</h2>
    <p>Irina Inina, {MAIL}</p>
  </>
);

const ru = (
  <>
    <p>Эти условия — соглашение между вами и Ириной Ининой, разработчиком WalkPick. Создавая аккаунт или пользуясь приложением, вы их принимаете. Если вы не согласны, пожалуйста, не пользуйтесь приложением.</p>

    <h2>Приложение</h2>
    <p>WalkPick записывает прогулки, считает шаги, хранит снимки, сделанные по дороге, и называет растения, птиц и места на них. Оно бесплатное и предлагается как есть, без обещания, что всегда будет доступно и без ошибок.</p>

    <h2>Ваш аккаунт</h2>
    <ul>
      <li>Для входа нужен адрес почты или аккаунт Google. Не теряйте к нему доступ: это единственный ключ к вашим данным.</li>
      <li>Вам должно быть не меньше 16 лет.</li>
      <li>Аккаунт можно удалить в любой момент в Profile.</li>
    </ul>

    <h2>Ваши материалы</h2>
    <p>Ваши прогулки, фото, заметки и записи здоровья остаются вашими. Вы разрешаете WalkPick хранить и обрабатывать их только чтобы показывать их вам и распознавать, что на снимках, как описано в <a href="/privacy">политике конфиденциальности</a>. Снимайте только там, где это разрешено, и уважайте частную жизнь других людей.</p>

    <h2>Честное использование</h2>
    <p>Не используйте приложение для нарушения закона, не пытайтесь добраться до чужих данных и не нагружайте сервис автоматическими запросами. Такой аккаунт может быть закрыт.</p>

    <h2>Распознавание и показатели здоровья</h2>
    <ul>
      <li>Названия растений, птиц и мест — предположения автоматических сервисов, и они могут быть ошибочными. Никогда не решайте по словам приложения, можно ли есть или трогать растение или гриб.</li>
      <li>Шаги, расстояние, калории и замечания о здоровье — оценки. WalkPick не медицинское изделие и не даёт медицинских советов; о здоровье говорите с врачом.</li>
      <li>Смотрите на дорогу, а не в экран. Вы гуляете на свой риск.</li>
    </ul>

    <h2>Другие сервисы</h2>
    <p>Приложение пользуется Google Maps, Google Sign-In, Pl@ntNet, Wikipedia и Open-Meteo. Их материалы принадлежат им и показываются на их условиях; описания из Wikipedia доступны по лицензии CC BY-SA. Птиц на телефоне распознаёт модель Google AIY Birds V1, используемая по лицензии {APACHE}.</p>

    <h2>Ответственность</h2>
    <p>В пределах, допустимых законом, разработчик не отвечает за потерю данных, за ошибки распознавания и показателей и за любой вред от использования приложения. Ничто здесь не ограничивает права, которые даёт вам как потребителю закон вашей страны.</p>

    <h2>Изменения и закрытие сервиса</h2>
    <p>Приложение и эти условия могут меняться; новый текст появится на этой странице с новой датой. Если сервис когда-нибудь закроется, вас предупредят заранее, чтобы вы успели сохранить свои данные.</p>

    <h2>Право</h2>
    <p>Эти условия регулируются законодательством Украины.</p>

    <h2>Контакты</h2>
    <p>Ирина Инина, {MAIL}</p>
  </>
);

export default function Terms() {
  return <Legal en={{ title: "Terms of Use", body: en }} ru={{ title: "Условия использования", body: ru }} />;
}
