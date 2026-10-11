// Shorten page: create short URLs with the optional feature fields.

document.addEventListener('DOMContentLoaded', function () {
  if (!requireAuth()) {
    return;
  }

  const features = {
    custom: { button: document.querySelector('[data-feature="custom"]'), field: document.getElementById('custom-field') },
    password: { button: document.querySelector('[data-feature="password"]'), field: document.getElementById('password-field') },
    expiry: { button: document.querySelector('[data-feature="expiry"]'), field: document.getElementById('expiry-field') },
    qr: { button: document.querySelector('[data-feature="qr"]'), field: null }
  };

  const alertBox = document.getElementById('shorten-alert');
  const resultBox = document.getElementById('result-box');
  const form = document.getElementById('shorten-form');

  function toggleFeature(name) {
    const feature = features[name];
    if (!feature) {
      return;
    }
    const selected = !feature.button.classList.contains('selected');
    feature.button.classList.toggle('selected', selected);
    if (feature.field) {
      feature.field.classList.toggle('hidden', !selected);
    }
    if (!selected && feature.field) {
      feature.field.querySelectorAll('input').forEach(function (input) {
        input.value = '';
      });
    }
  }

  Object.keys(features).forEach(function (name) {
    features[name].button.addEventListener('click', function () {
      toggleFeature(name);
    });
  });

  // Preselect a feature when arriving from the home page cards.
  const params = new URLSearchParams(window.location.search);
  const wanted = params.get('feature');
  if (wanted && features[wanted]) {
    toggleFeature(wanted);
  }

  // Prefill the long URL coming from the home page.
  const pending = localStorage.getItem('linkshort_pending_url');
  if (pending) {
    document.getElementById('original-url').value = pending;
    localStorage.removeItem('linkshort_pending_url');
  }

  function toApiDateTime(value) {
    if (!value) {
      return null;
    }
    // "YYYY-MM-DDTHH:mm" -> "YYYY-MM-DDTHH:mm:00"
    return value.length === 16 ? value + ':00' : value;
  }

  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    alertBox.className = 'alert';
    resultBox.classList.remove('show');

    const payload = {
      originalUrl: document.getElementById('original-url').value.trim()
    };

    if (features.custom.button.classList.contains('selected')) {
      payload.customAlias = document.getElementById('custom-alias').value.trim();
    }

    if (features.password.button.classList.contains('selected')) {
      payload.password = document.getElementById('url-password').value.trim();
    }

    if (features.expiry.button.classList.contains('selected')) {
      const expiry = toApiDateTime(document.getElementById('expiry-date').value);
      if (expiry) {
        payload.expiryDate = expiry;
      }
    }

    try {
      const url = await Api.post('/api/urls', payload);
      showResult(url);
    } catch (error) {
      alertBox.textContent = formatError(error);
      alertBox.className = 'alert show alert-error';
    }
  });

  function showResult(url) {
    const link = document.getElementById('result-link');
    link.href = url.shortUrl;
    link.textContent = url.shortUrl;

    const analyticsLink = document.getElementById('result-analytics-link');
    analyticsLink.href = 'analytics.html?id=' + url.id;
    analyticsLink.classList.remove('hidden');

    const qrLink = document.getElementById('result-qr-link');
    const qrImage = document.getElementById('result-qr');
    qrImage.style.display = 'none';

    if (features.qr.button.classList.contains('selected')) {
      qrLink.href = 'qr-code.html?id=' + url.id;
      qrLink.classList.remove('hidden');
      loadQrCode(url.id, qrImage);
    } else {
      qrLink.classList.add('hidden');
    }

    document.getElementById('copy-btn').onclick = function () {
      copyText(url.shortUrl);
      this.textContent = 'Copied';
      const button = this;
      setTimeout(function () {
        button.textContent = 'Copy Link';
      }, 1500);
    };

    resultBox.classList.add('show');
    resultBox.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  async function loadQrCode(urlId, image) {
    try {
      const blob = await Api.blob('/api/urls/' + urlId + '/qr');
      image.src = URL.createObjectURL(blob);
      image.style.display = 'inline-block';
    } catch (error) {
      // The short URL is still created, the QR preview is only a bonus.
    }
  }
});
