// QR Code page: generate and download a QR image for an existing short URL.

document.addEventListener('DOMContentLoaded', async function () {
  if (!requireAuth()) {
    return;
  }

  const alertBox = document.getElementById('qr-alert');
  const select = document.getElementById('url-select');
  const image = document.getElementById('qr-image');
  const empty = document.getElementById('qr-empty');
  const downloadBtn = document.getElementById('download-btn');
  const generateBtn = document.getElementById('generate-btn');

  let currentUrl = null;
  let currentObjectUrl = null;

  function setAlert(message, type) {
    alertBox.textContent = message;
    alertBox.className = 'alert show ' + (type === 'success' ? 'alert-success' : 'alert-error');
  }

  try {
    const urls = await Api.get('/api/urls');

    if (!urls.length) {
      select.innerHTML = '<option value="">No short URLs yet</option>';
      return;
    }

    select.innerHTML = '';
    urls.forEach(function (url) {
      const option = document.createElement('option');
      option.value = url.id;
      option.textContent = url.shortUrl + (url.title ? ' - ' + url.title : '');
      select.appendChild(option);
    });

    const params = new URLSearchParams(window.location.search);
    const wanted = params.get('id');
    if (wanted && urls.some(function (url) { return String(url.id) === wanted; })) {
      select.value = wanted;
    }

    currentUrl = urls.find(function (url) { return String(url.id) === select.value; }) || null;

    select.addEventListener('change', function () {
      currentUrl = urls.find(function (url) { return String(url.id) === this.value; }.bind(this)) || null;
    });
  } catch (error) {
    if (error.status !== 401) {
      setAlert(formatError(error));
    }
    select.innerHTML = '<option value="">Could not load URLs</option>';
  }

  generateBtn.addEventListener('click', async function () {
    const urlId = select.value;
    alertBox.className = 'alert';

    if (!urlId) {
      setAlert('Please select a short URL first');
      return;
    }

    try {
      const blob = await Api.blob('/api/urls/' + urlId + '/qr');

      if (currentObjectUrl) {
        URL.revokeObjectURL(currentObjectUrl);
      }
      currentObjectUrl = URL.createObjectURL(blob);

      image.src = currentObjectUrl;
      image.classList.add('show');
      empty.classList.add('hidden');

      const fileName = currentUrl && currentUrl.shortCode
        ? 'linkshort-' + currentUrl.shortCode + '.png'
        : 'linkshort-qr.png';
      downloadBtn.href = currentObjectUrl;
      downloadBtn.setAttribute('download', fileName);
      downloadBtn.classList.remove('hidden');
    } catch (error) {
      setAlert(formatError(error));
    }
  });
});
