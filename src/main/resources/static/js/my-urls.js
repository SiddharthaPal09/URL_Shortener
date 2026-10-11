// My URLs page: list, edit and delete the short URLs of the logged in user.

document.addEventListener('DOMContentLoaded', async function () {
  if (!requireAuth()) {
    return;
  }

  const alertBox = document.getElementById('urls-alert');
  const tableBox = document.getElementById('urls-table');
  const editModal = document.getElementById('edit-modal');
  const editAlert = document.getElementById('edit-alert');

  function setAlert(element, message, type) {
    element.textContent = message;
    element.className = 'alert show ' + (type === 'success' ? 'alert-success' : 'alert-error');
  }

  function clearAlert(element) {
    element.className = 'alert';
    element.textContent = '';
  }

  async function loadUrls() {
    clearAlert(alertBox);
    tableBox.innerHTML = '<p class="muted">Loading...</p>';

    try {
      const urls = await Api.get('/api/urls');
      render(urls);
    } catch (error) {
      if (error.status !== 401) {
        setAlert(alertBox, formatError(error), 'error');
      }
      tableBox.innerHTML = '<p class="empty-state">Could not load your URLs.</p>';
    }
  }

  function render(urls) {
    if (!urls.length) {
      tableBox.innerHTML =
        '<div class="empty-state">You have no short URLs yet. <a href="shorten.html">Shorten your first URL</a>.</div>';
      return;
    }

    let html =
      '<table><thead><tr>' +
      '<th>Short URL</th><th>Original URL</th><th>Clicks</th><th>Expiry</th><th>Status</th><th>Actions</th>' +
      '</tr></thead><tbody>';

    urls.forEach(function (url) {
      html +=
        '<tr>' +
        '<td><a href="' + escapeHtml(url.shortUrl) + '" target="_blank" rel="noopener">' + escapeHtml(url.shortUrl) + '</a>' +
        (url.passwordProtected ? ' <span class="badge locked">Protected</span>' : '') + '</td>' +
        '<td class="url-cell">' + escapeHtml(url.originalUrl) + '</td>' +
        '<td>' + url.clickCount + '</td>' +
        '<td>' + escapeHtml(formatDate(url.expiryDate)) + '</td>' +
        '<td>' + statusBadge(url) + '</td>' +
        '<td class="row" style="gap: 6px;">' +
        '<button type="button" class="btn btn-sm btn-blue" data-edit="' + url.id + '">Edit</button>' +
        '<button type="button" class="btn btn-sm btn-danger" data-delete="' + url.id + '">Delete</button>' +
        '</td>' +
        '</tr>';
    });

    html += '</tbody></table>';
    tableBox.innerHTML = html;

    tableBox.querySelectorAll('[data-edit]').forEach(function (button) {
      button.addEventListener('click', function () {
        openEdit(Number(this.getAttribute('data-edit')), urls);
      });
    });

    tableBox.querySelectorAll('[data-delete]').forEach(function (button) {
      button.addEventListener('click', function () {
        removeUrl(Number(this.getAttribute('data-delete')));
      });
    });
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

  function toLocalInputValue(value) {
    if (!value) {
      return '';
    }
    const date = new Date(value);
    if (isNaN(date.getTime())) {
      return '';
    }
    const pad = function (n) { return n < 10 ? '0' + n : n; };
    return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) +
      'T' + pad(date.getHours()) + ':' + pad(date.getMinutes());
  }

  function openEdit(id, urls) {
    const url = urls.find(function (item) { return item.id === id; });
    if (!url) {
      return;
    }

    clearAlert(editAlert);
    document.getElementById('edit-id').value = url.id;
    document.getElementById('edit-original-url').value = url.originalUrl;
    document.getElementById('edit-title').value = url.title || '';
    document.getElementById('edit-description').value = url.description || '';
    document.getElementById('edit-expiry').value = toLocalInputValue(url.expiryDate);
    editModal.classList.add('show');
  }

  document.getElementById('edit-cancel').addEventListener('click', function () {
    editModal.classList.remove('show');
  });

  editModal.addEventListener('click', function (event) {
    if (event.target === editModal) {
      editModal.classList.remove('show');
    }
  });

  document.getElementById('edit-form').addEventListener('submit', async function (event) {
    event.preventDefault();
    clearAlert(editAlert);

    const id = document.getElementById('edit-id').value;
    const expiry = document.getElementById('edit-expiry').value;
    const payload = {
      originalUrl: document.getElementById('edit-original-url').value.trim(),
      title: document.getElementById('edit-title').value.trim(),
      description: document.getElementById('edit-description').value.trim()
    };

    if (expiry) {
      payload.expiryDate = expiry.length === 16 ? expiry + ':00' : expiry;
    }

    try {
      await Api.put('/api/urls/' + id, payload);
      editModal.classList.remove('show');
      await loadUrls();
      setAlert(alertBox, 'Short URL updated', 'success');
    } catch (error) {
      setAlert(editAlert, formatError(error), 'error');
    }
  });

  async function removeUrl(id) {
    if (!window.confirm('Delete this short URL? This cannot be undone.')) {
      return;
    }

    try {
      await Api.del('/api/urls/' + id);
      await loadUrls();
      setAlert(alertBox, 'Short URL deleted', 'success');
    } catch (error) {
      setAlert(alertBox, formatError(error), 'error');
    }
  }

  await loadUrls();
});
