// Home page: hand the long URL over to the shorten page.

document.addEventListener('DOMContentLoaded', function () {
  const form = document.getElementById('home-form');
  if (!form) {
    return;
  }

  form.addEventListener('submit', function (event) {
    event.preventDefault();

    const longUrl = document.getElementById('long-url').value.trim();
    if (!longUrl) {
      return;
    }

    localStorage.setItem('linkshort_pending_url', longUrl);

    if (Api.isLoggedIn()) {
      window.location.href = 'shorten.html';
    } else {
      window.location.href = 'login.html';
    }
  });
});
