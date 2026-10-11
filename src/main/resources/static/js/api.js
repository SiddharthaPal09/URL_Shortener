// Shared API helper for LinkShort. All calls use relative URLs on the same port.

const Api = (function () {
  const TOKEN_KEY = 'linkshort_token';
  const USER_KEY = 'linkshort_user';

  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function setSession(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user || null));
  }

  function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  function getUser() {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY) || 'null');
    } catch (e) {
      return null;
    }
  }

  function isLoggedIn() {
    return !!getToken();
  }

  async function request(path, options) {
    options = options || {};
    const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});

    const token = getToken();
    if (token && options.auth !== false) {
      headers['Authorization'] = 'Bearer ' + token;
    }

    const response = await fetch(path, {
      method: options.method || 'GET',
      headers: headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined
    });

    if (response.status === 401 && !options.skipAuthRedirect) {
      clearSession();
      if (!window.location.pathname.endsWith('login.html')) {
        window.location.href = 'login.html';
      }
      throw new Error('Your session has expired, please log in again');
    }

    if (response.status === 204) {
      return null;
    }

    const contentType = response.headers.get('Content-Type') || '';
    const isJson = contentType.indexOf('application/json') !== -1;
    const data = isJson ? await response.json() : await response.blob();

    if (!response.ok) {
      const message = isJson && data && data.message
        ? data.message
        : 'Request failed with status ' + response.status;
      const error = new Error(message);
      error.status = response.status;
      throw error;
    }

    return data;
  }

  return {
    getToken: getToken,
    setSession: setSession,
    clearSession: clearSession,
    getUser: getUser,
    isLoggedIn: isLoggedIn,
    get: function (path, options) { return request(path, Object.assign({ method: 'GET' }, options)); },
    post: function (path, body, options) { return request(path, Object.assign({ method: 'POST', body: body }, options)); },
    put: function (path, body, options) { return request(path, Object.assign({ method: 'PUT', body: body }, options)); },
    del: function (path, options) { return request(path, Object.assign({ method: 'DELETE' }, options)); },
    // Binary endpoints (QR images) need their own handling because of the auth header.
    async blob(path) {
      const headers = {};
      const token = getToken();
      if (token) {
        headers['Authorization'] = 'Bearer ' + token;
      }
      const response = await fetch(path, { headers: headers });
      if (response.status === 401) {
        clearSession();
        window.location.href = 'login.html';
        throw new Error('Your session has expired, please log in again');
      }
      if (!response.ok) {
        let message = 'Could not load the image';
        try {
          const data = await response.json();
          if (data && data.message) {
            message = data.message;
          }
        } catch (e) { /* keep default message */ }
        throw new Error(message);
      }
      return await response.blob();
    }
  };
})();
