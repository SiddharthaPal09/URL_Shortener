// Profile page: load the current user and update name, email and password.

document.addEventListener('DOMContentLoaded', async function () {
  if (!requireAuth()) {
    return;
  }

  const alertBox = document.getElementById('profile-alert');
  const form = document.getElementById('profile-form');

  function setAlert(message, type) {
    alertBox.textContent = message;
    alertBox.className = 'alert show ' + (type === 'success' ? 'alert-success' : 'alert-error');
  }

  try {
    const user = await Api.get('/api/auth/me');
    Api.setSession(Api.getToken(), user);
    document.getElementById('name').value = user.name || '';
    document.getElementById('email').value = user.email || '';
  } catch (error) {
    if (error.status !== 401) {
      setAlert(formatError(error), 'error');
    }
    return;
  }

  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    alertBox.className = 'alert';

    const name = document.getElementById('name').value.trim();
    const email = document.getElementById('email').value.trim();
    const newPassword = document.getElementById('new-password').value;
    const confirmPassword = document.getElementById('confirm-password').value;

    const payload = { name: name, email: email };

    if (newPassword) {
      if (newPassword !== confirmPassword) {
        setAlert('New password and confirmation do not match', 'error');
        return;
      }
      payload.newPassword = newPassword;
      payload.confirmPassword = confirmPassword;
    }

    try {
      const user = await Api.put('/api/auth/me', payload);
      Api.setSession(Api.getToken(), user);
      document.getElementById('new-password').value = '';
      document.getElementById('confirm-password').value = '';
      setAlert('Profile updated', 'success');
    } catch (error) {
      setAlert(formatError(error), 'error');
    }
  });
});
