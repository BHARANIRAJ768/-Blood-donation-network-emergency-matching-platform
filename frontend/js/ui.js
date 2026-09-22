/* =========================================================================
 * VitalDrop - shared UI helpers: toasts, spinner, theme, escape, formatting
 * ========================================================================= */

function esc(value) {
    if (value === null || value === undefined) return '';
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function initials(name) {
    if (!name) return '?';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function avatarHtml(name, photo, sizeClass) {
    const cls = sizeClass || 'avatar-round';
    if (photo) {
        return `<img src="${esc(photo)}" alt="${esc(name)}" class="${cls}">`;
    }
    return `<div class="avatar-initials ${cls.includes('avatar-round') ? '' : ''}">${esc(initials(name))}</div>`;
}

/* --------------------------- toasts --------------------------- */

function showToast(message, type) {
    type = type || 'info';
    const icons = { success: '&#10004;', error: '&#10060;', warning: '&#9888;', info: '&#8505;' };
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container-lifelink';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = 'toast-lifelink ' + type;
    toast.innerHTML = `
        <span class="toast-icon">${icons[type] || icons.info}</span>
        <div class="toast-text">${esc(message)}</div>
        <button class="toast-close" aria-label="Close">&times;</button>`;
    const close = () => {
        toast.style.animation = 'toastOut 0.25s ease forwards';
        setTimeout(() => toast.remove(), 260);
    };
    toast.querySelector('.toast-close').addEventListener('click', close);
    container.appendChild(toast);
    setTimeout(close, 4500);
}

/* --------------------------- spinner --------------------------- */

function showSpinner(show) {
    let overlay = document.getElementById('spinnerOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'spinnerOverlay';
        overlay.className = 'spinner-overlay';
        overlay.innerHTML = '<div class="spinner-lifelink"></div>';
        document.body.appendChild(overlay);
    }
    overlay.classList.toggle('show', !!show);
}

/* --------------------------- theme --------------------------- */

function initTheme() {
    const saved = localStorage.getItem('ll_theme');
    if (saved === 'dark' || (!saved && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
        document.documentElement.setAttribute('data-theme', 'dark');
    } else {
        document.documentElement.removeAttribute('data-theme');
    }
    const toggle = document.getElementById('themeToggle');
    if (toggle) {
        toggle.innerHTML = document.documentElement.getAttribute('data-theme') === 'dark'
            ? '<i class="bi bi-sun-fill"></i>' : '<i class="bi bi-moon-fill"></i>';
    }
}

function toggleTheme() {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    if (isDark) {
        document.documentElement.removeAttribute('data-theme');
        localStorage.setItem('ll_theme', 'light');
    } else {
        document.documentElement.setAttribute('data-theme', 'dark');
        localStorage.setItem('ll_theme', 'dark');
    }
    const toggle = document.getElementById('themeToggle');
    if (toggle) {
        toggle.innerHTML = isDark ? '<i class="bi bi-moon-fill"></i>' : '<i class="bi bi-sun-fill"></i>';
    }
}

/* --------------------------- formatting --------------------------- */

function formatDate(value) {
    if (!value) return '-';
    return new Date(value).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function formatDateTime(value) {
    if (!value) return '-';
    return new Date(value).toLocaleString(undefined, {
        year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
    });
}

function timeAgo(value) {
    if (!value) return '';
    const seconds = Math.floor((Date.now() - new Date(value).getTime()) / 1000);
    if (seconds < 60) return 'just now';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return minutes + ' min ago';
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return hours + ' hr ago';
    const days = Math.floor(hours / 24);
    if (days < 30) return days + ' day' + (days > 1 ? 's' : '') + ' ago';
    return formatDate(value);
}

function statusBadge(status) {
    const map = {
        PENDING: '&#9203; Pending',
        ACCEPTED: '&#9989; Accepted',
        COMPLETED: '&#10004; Completed',
        REJECTED: '&#10060; Rejected',
        CANCELLED: '&#10007; Cancelled',
    };
    return `<span class="status-badge status-${esc(status)}">${map[status] || esc(status)}</span>`;
}

function roleBadge(role) {
    return `<span class="role-badge role-${esc(role)}">${esc(role)}</span>`;
}

function distanceText(km) {
    if (km === null || km === undefined) return 'Distance n/a';
    return km < 1 ? `${Math.round(km * 1000)} m away` : `${km.toFixed(1)} km away`;
}

function setBusy(loaderId, textId, busy, text) {
    const loader = document.getElementById(loaderId);
    if (loader) loader.classList.toggle('show', !!busy);
    const textEl = document.getElementById(textId);
    if (!textEl) return;
    if (text) textEl.innerHTML = text;
    textEl.style.opacity = busy ? 0.6 : 1;
}
