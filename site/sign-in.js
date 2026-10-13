if (live.signedIn()) location.replace('walk.html');

const form = document.querySelector('.entry');
const field = form.querySelector('input');
const button = form.querySelector('button');
const hint = form.querySelector('.hint');
let email = '';

document.querySelector('.sign-in-form > .button').addEventListener('click', event => {
  event.preventDefault();
  live.google();
});

// One field, two steps: the address first, then the code from the letter
const step = async () => {
  button.disabled = true;
  try {
    if (!email) {
      await live.auth('otp', { email: field.value.trim(), create_user: false });
      email = field.value.trim();
      field.value = '';
      field.type = 'text';
      field.inputMode = 'numeric';
      field.placeholder = t('Code from the email', 'Код из письма');
      button.textContent = t('Sign in', 'Войти');
      hint.textContent = t(`The code went to ${email}.`, `Код отправлен на ${email}.`);
    } else {
      live.keep(await live.auth('verify', { type: 'email', email, token: field.value.trim() }));
      location.replace('walk.html');
    }
  } catch (problem) {
    hint.textContent = problem.message;
  }
  button.disabled = false;
};
button.addEventListener('click', step);
form.addEventListener('submit', event => {
  event.preventDefault();
  step();
});
