// Analytics page: per URL click stats from the backend.

document.addEventListener('DOMContentLoaded', async function () {
  if (!requireAuth()) {
    return;
  }

  const alertBox = document.getElementById('analytics-alert');
  const select = document.getElementById('url-select');
  const content = document.getElementById('analytics-content');
  const empty = document.getElementById('analytics-empty');

  function setAlert(message) {
    alertBox.textContent = message;
    alertBox.className = 'alert show alert-error';
  }

  try {
    const urls = await Api.get('/api/urls');

    if (!urls.length) {
      select.innerHTML = '<option value="">No short URLs yet</option>';
      empty.classList.remove('hidden');
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

    await loadAnalytics(select.value);

    select.addEventListener('change', function () {
      loadAnalytics(this.value);
    });
  } catch (error) {
    if (error.status !== 401) {
      setAlert(formatError(error));
    }
    select.innerHTML = '<option value="">Could not load URLs</option>';
    empty.classList.remove('hidden');
  }

  async function loadAnalytics(urlId) {
    if (!urlId) {
      content.classList.add('hidden');
      empty.classList.remove('hidden');
      return;
    }

    try {
      const data = await Api.get('/api/urls/' + urlId + '/analytics');
      empty.classList.add('hidden');
      content.classList.remove('hidden');

      document.getElementById('total-clicks').textContent = data.totalClicks;
      document.getElementById('analytics-short-url').textContent = data.shortUrl;
      document.getElementById('analytics-created').textContent = formatDate(data.createdAt);
      document.getElementById('analytics-expiry').textContent = formatDate(data.expiryDate);

      renderChart(data.clicksOverTime || {});
      renderBreakdown('browser-table', 'Browser', 'Clicks', data.browsers || {}, 'No browser data recorded yet.');
      renderBreakdown('devices-table', 'Value', 'Clicks', mergeMaps(data.devices || {}, data.operatingSystems || {}), 'No device data recorded yet.');
    } catch (error) {
      setAlert(formatError(error));
    }
  }

  function mergeMaps(devices, operatingSystems) {
    const merged = {};
    Object.keys(devices).forEach(function (key) {
      merged['Device: ' + key] = devices[key];
    });
    Object.keys(operatingSystems).forEach(function (key) {
      merged['OS: ' + key] = operatingSystems[key];
    });
    return merged;
  }

  function renderChart(clicksOverTime) {
    const chart = document.getElementById('click-chart');
    const days = Object.keys(clicksOverTime).sort();

    if (!days.length) {
      chart.innerHTML = '<p class="muted" style="margin: auto;">No clicks recorded yet.</p>';
      return;
    }

    const max = Math.max.apply(null, days.map(function (day) { return clicksOverTime[day]; }));

    let html = '';
    days.forEach(function (day) {
      const value = clicksOverTime[day];
      const height = max > 0 ? Math.round((value / max) * 140) : 2;
      html +=
        '<div class="bar-col">' +
        '<span class="bar-value">' + value + '</span>' +
        '<div class="bar-fill" style="height: ' + height + 'px;"></div>' +
        '<span class="bar-label">' + escapeHtml(day.slice(5)) + '</span>' +
        '</div>';
    });

    chart.innerHTML = html;
  }

  function renderBreakdown(elementId, firstColumn, secondColumn, data, emptyMessage) {
    const box = document.getElementById(elementId);
    const keys = Object.keys(data);

    if (!keys.length) {
      box.innerHTML = '<p class="empty-state">' + escapeHtml(emptyMessage) + '</p>';
      return;
    }

    let html =
      '<table><thead><tr><th>' + escapeHtml(firstColumn) + '</th><th>' + escapeHtml(secondColumn) + '</th></tr></thead><tbody>';

    keys.forEach(function (key) {
      html += '<tr><td>' + escapeHtml(key) + '</td><td>' + data[key] + '</td></tr>';
    });

    html += '</tbody></table>';
    box.innerHTML = html;
  }
});
