/* =========================================================================
 * VitalDrop - REST API client + JWT session handling
 * ========================================================================= */

const API_BASE_URL = 'http://localhost:8080';

class ApiError extends Error {
    constructor(message, status, data) {
        super(message);
        this.status = status;
        this.data = data;
    }
}

const API = {
    getToken() {
        return localStorage.getItem('ll_token') || sessionStorage.getItem('ll_token');
    },

    setToken(token, remember) {
        const store = remember ? localStorage : sessionStorage;
        store.setItem('ll_token', token);
    },

    clearToken() {
        localStorage.removeItem('ll_token');
        sessionStorage.removeItem('ll_token');
    },

    isAuthenticated() {
        return !!this.getToken();
    },

    async request(method, path, body) {
        const headers = { 'Content-Type': 'application/json' };
        const token = this.getToken();
        if (token) {
            headers['Authorization'] = 'Bearer ' + token;
        }

        let response;
        try {
            response = await fetch(API_BASE_URL + path, {
                method,
                headers,
                body: body !== undefined ? JSON.stringify(body) : undefined,
            });
        } catch (err) {
            throw new ApiError('Cannot reach the server. Is VitalDrop running?', 0, null);
        }

        if (response.status === 401 && !path.startsWith('/api/auth/login')) {
            this.clearToken();
            window.location.href = 'login.html?expired=1';
            throw new ApiError('Session expired, please log in again', 401, null);
        }

        const contentType = response.headers.get('content-type') || '';
        const data = contentType.includes('application/json') ? await response.json() : null;

        if (!response.ok) {
            const message = (data && (data.message || data.error)) || 'Request failed';
            throw new ApiError(message, response.status, data);
        }
        return data;
    },

    get(path) {
        return this.request('GET', path);
    },

    post(path, body) {
        return this.request('POST', path, body);
    },

    put(path, body) {
        return this.request('PUT', path, body);
    },
};
