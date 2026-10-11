// Shared navbar behaviour plus the login and register forms.

function showAuthAlert(element, message, type) {
  if (!element) {
    return;
  }
  element.textContent = message;
  element.className = 'alert show ' + (type === 'success' ? 'alert-success' : 'alert-error');
}

function hideAuthAlert(element) {
  if (!element) {
    return;
  }
  element.className = 'alert';
  element.textContent = '';
}

function renderNavbar() {
  const guestLinks = document.querySelectorAll('[data-auth="guest"]');
  const userLinks = document.querySelectorAll('[data-auth="user"]');

  guestLinks.forEach(function (el) {
    el.classList.toggle('hidden', Api.isLoggedIn());
  });

  userLinks.forEach(function (el) {
    el.classList.toggle('hidden', !Api.isLoggedIn());
  });

  const page = window.location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-links a[data-page]').forEach(function (link) {
    link.classList.toggle('active', link.getAttribute('data-page') === page);
  });
}

function requireAuth() {
  if (!Api.isLoggedIn()) {
    window.location.replace('login.html');
    return false;
  }
  return true;
}

function redirectIfLoggedIn() {
  if (Api.isLoggedIn()) {
    window.location.replace('dashboard.html');
    return true;
  }
  return false;
}

function handleLogout(event) {
  if (event) {
    event.preventDefault();
  }
  Api.clearSession();
  window.location.href = 'index.html';
}

function setupLoginForm() {
  const form = document.getElementById('login-form');
  if (!form) {
    return;
  }

  const alertBox = document.getElementById('login-alert');

  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    hideAuthAlert(alertBox);

    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    try {
      const response = await Api.post('/api/auth/login', { email: email, password: password }, { skipAuthRedirect: true });
      Api.setSession(response.token, response.user);
      window.location.href = 'dashboard.html';
    } catch (error) {
      showAuthAlert(alertBox, error.message, 'error');
    }
  });
}

function setupRegisterForm() {
  const form = document.getElementById('register-form');
  if (!form) {
    return;
  }

  const alertBox = document.getElementById('register-alert');

  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    hideAuthAlert(alertBox);

    const name = document.getElementById('name').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const confirm = document.getElementById('confirm-password').value;

    if (password !== confirm) {
      showAuthAlert(alertBox, 'Password and confirm password do not match', 'error');
      return;
    }

    try {
      const response = await Api.post('/api/auth/register', { name: name, email: email, password: password }, { skipAuthRedirect: true });
      Api.setSession(response.token, response.user);
      window.location.href = 'dashboard.html';
    } catch (error) {
      showAuthAlert(alertBox, error.message, 'error');
    }
  });
}

function formatError(error) {
  return error && error.message ? error.message : 'Something went wrong, please try again';
}

function formatDate(value) {
  if (!value) {
    return 'Never';
  }
  const date = new Date(value);
  if (isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function escapeHtml(value) {
  return String(value === null || value === undefined ? '' : value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function copyText(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    return navigator.clipboard.writeText(text);
  }
  const input = document.createElement('textarea');
  input.value = text;
  document.body.appendChild(input);
  input.select();
  document.execCommand('copy');
  document.body.removeChild(input);
  return Promise.resolve();
}

document.addEventListener('DOMContentLoaded', function () {
  renderNavbar();

  document.querySelectorAll('[data-action="logout"]').forEach(function (el) {
    el.addEventListener('click', handleLogout);
  });

  setupLoginForm();
  setupRegisterForm();
});
