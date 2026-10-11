// Dashboard page: real statistics and the most recent short URLs.

document.addEventListener('DOMContentLoaded', async function () {
  if (!requireAuth()) {
    return;
  }

  const alertBox = document.getElementById('dashboard-alert');
  const recentBox = document.getElementById('recent-urls');

  try {
    const stats = await Api.get('/api/urls/stats');
    document.getElementById('stat-total-urls').textContent = stats.totalUrls;
    document.getElementById('stat-total-clicks').textContent = stats.totalClicks;
    document.getElementById('stat-active-urls').textContent = stats.activeUrls;
    document.getElementById('stat-expired-urls').textContent = stats.expiredUrls;
  } catch (error) {
    alertBox.textContent = formatError(error);
    alertBox.className = 'alert show alert-error';
  }

  try {
    const urls = await Api.get('/api/urls');
    renderRecent(urls.slice(0, 5));
  } catch (error) {
    recentBox.innerHTML = '<p class="empty-state">' + escapeHtml(formatError(error)) + '</p>';
  }

  function renderRecent(urls) {
    if (!urls.length) {
      recentBox.innerHTML =
        '<div class="empty-state">No short URLs yet. <a href="shorten.html">Create your first one</a>.</div>';
      return;
    }

    let html =
      '<table><thead><tr>' +
      '<th>Short URL</th><th>Original URL</th><th>Clicks</th><th>Expiry</th><th>Status</th>' +
      '</tr></thead><tbody>';

    urls.forEach(function (url) {
      html +=
        '<tr>' +
        '<td><a href="' + escapeHtml(url.shortUrl) + '" target="_blank" rel="noopener">' + escapeHtml(url.shortUrl) + '</a></td>' +
        '<td class="url-cell">' + escapeHtml(url.originalUrl) + '</td>' +
        '<td>' + url.clickCount + '</td>' +
        '<td>' + escapeHtml(formatDate(url.expiryDate)) + '</td>' +
        '<td>' + statusBadge(url) + '</td>' +
        '</tr>';
    });

    html += '</tbody></table>';
    recentBox.innerHTML = html;
  }

  function statusBadge(url) {
    if (!url.active) {
      return '<span class="badge">Inactive</span>';
    }
    const expired = url.expiryDate && new Date(url.expiryDate) < new Date();
    if (expired) {
      return '<span class="badge expired">Expired</span>';
    }
    return '<span class="badge active">Active</span>';
  }
});
