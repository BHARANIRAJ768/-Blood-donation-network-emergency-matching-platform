/* =========================================================================
 * VitalDrop - admin dashboard: stats, users, requests, history
 * ========================================================================= */

let adminFilters = { users: { role: '', search: '' }, requests: { status: '', search: '' }, history: { status: '', search: '' } };

document.addEventListener('DOMContentLoaded', async () => {
    if (!requireAuth()) return;
    showSpinner(true);
    try {
        await loadUser();
        if (currentUser.role !== 'ADMIN') {
            showToast('You do not have access to this page', 'error');
            setTimeout(() => (window.location.href = 'home.html'), 800);
            return;
        }
        await setupShell('admin');
        setupAdmin();
        await Promise.all([loadStats(), loadUsers(0), loadRequests(0), loadHistory2(0)]);
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
});

function setupAdmin() {
    document.getElementById('usersSearchBtn').addEventListener('click', () => {
        adminFilters.users = { role: document.getElementById('usersRole').value, search: document.getElementById('usersSearch').value.trim() };
        loadUsers(0);
    });
    document.getElementById('requestsSearchBtn').addEventListener('click', () => {
        adminFilters.requests = { status: document.getElementById('requestsStatus').value, search: document.getElementById('requestsSearch').value.trim() };
        loadRequests(0);
    });
    document.getElementById('historySearchBtn2').addEventListener('click', () => {
        adminFilters.history = { status: document.getElementById('historyStatus2').value, search: document.getElementById('historySearch2').value.trim() };
        loadHistory2(0);
    });
    document.getElementById('adminExportPdf').addEventListener('click', exportAdminPdf);
}

async function loadStats() {
    try {
        const s = await API.get('/api/admin/stats');
        document.getElementById('statUsers').textContent = s.totalUsers;
        document.getElementById('statDonors').textContent = s.totalDonors;
        document.getElementById('statPatients').textContent = s.totalPatients;
        document.getElementById('statActiveDonors').textContent = s.activeDonors;
        document.getElementById('statPending').textContent = s.pendingRequests;
        document.getElementById('statCompleted').textContent = s.totalDonations;
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function loadUsers(page) {
    showSpinner(true);
    try {
        const f = adminFilters.users;
        const params = new URLSearchParams({ page: String(page), size: '10' });
        if (f.role) params.set('role', f.role);
        if (f.search) params.set('search', f.search);
        const data = await API.get('/api/admin/users?' + params.toString());
        document.getElementById('usersBody').innerHTML = data.content.length ? data.content.map(u => `
            <tr>
                <td class="fw-semibold">${esc(u.name)}</td>
                <td>${esc(u.email)}</td>
                <td>${esc(u.phone)}</td>
                <td>${roleBadge(u.role)}</td>
                <td>${u.bloodGroup ? `<span class="bg-chip">${esc(u.bloodGroup)}</span>` : '—'}</td>
                <td>${esc(u.district || '—')}</td>
                <td>${u.available ? '<span class="text-success fw-bold">Yes</span>' : '<span class="text-muted">No</span>'}</td>
                <td class="text-nowrap">${formatDate(u.createdAt)}</td>
            </tr>`).join('') : emptyRow(8);
        renderPagination(data, 'usersPagination', (p) => loadUsers(p));
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
}

async function loadRequests(page) {
    showSpinner(true);
    try {
        const f = adminFilters.requests;
        const params = new URLSearchParams({ page: String(page), size: '10' });
        if (f.status) params.set('status', f.status);
        if (f.search) params.set('search', f.search);
        const data = await API.get('/api/admin/requests?' + params.toString());
        document.getElementById('requestsBody').innerHTML = data.content.length ? data.content.map(r => `
            <tr>
                <td>#${r.id}</td>
                <td><span class="bg-chip">${esc(r.bloodGroup)}</span></td>
                <td>${esc(r.patientName)}${r.emergency ? ' <span class="emergency-badge">Emergency</span>' : ''}</td>
                <td>${esc(r.donorName || '—')}</td>
                <td>${esc(r.hospitalName)}</td>
                <td>${r.units}</td>
                <td>${statusBadge(r.status)}</td>
                <td class="text-nowrap">${formatDateTime(r.createdAt)}</td>
            </tr>`).join('') : emptyRow(8);
        renderPagination(data, 'requestsPagination', (p) => loadRequests(p));
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
}

async function loadHistory2(page) {
    showSpinner(true);
    try {
        const f = adminFilters.history;
        const params = new URLSearchParams({ page: String(page), size: '10' });
        if (f.status) params.set('status', f.status);
        if (f.search) params.set('search', f.search);
        const data = await API.get('/api/admin/history?' + params.toString());
        document.getElementById('historyBody2').innerHTML = data.content.length ? data.content.map(h => `
            <tr>
                <td class="text-nowrap">${h.status === 'COMPLETED' && h.donationDate ? formatDate(h.donationDate) : formatDate(h.createdAt)}</td>
                <td><span class="bg-chip">${esc(h.bloodGroup || '—')}</span></td>
                <td>${esc(h.patientName || '—')}</td>
                <td>${esc(h.donorName || '—')}</td>
                <td>${esc(h.hospitalName || '—')}</td>
                <td>${statusBadge(h.status)}</td>
            </tr>`).join('') : emptyRow(6);
        renderPagination(data, 'historyPagination2', (p) => loadHistory2(p));
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
}

function emptyRow(colspan) {
    return `<tr><td colspan="${colspan}"><div class="empty-state"><div class="empty-icon">&#128269;</div><div>No records found.</div></div></td></tr>`;
}

async function fetchAllAdmin(path, filters, pageSize) {
    const rows = [];
    let page = 0;
    for (;;) {
        const params = new URLSearchParams({ page: String(page), size: String(pageSize) });
        Object.entries(filters).forEach(([k, v]) => { if (v) params.set(k, v); });
        const data = await API.get(path + '?' + params.toString());
        rows.push(...data.content);
        if (page + 1 >= data.totalPages) break;
        page += 1;
    }
    return rows;
}

async function exportAdminPdf() {
    showSpinner(true);
    try {
        const f = adminFilters.history;
        const rows = await fetchAllAdmin('/api/admin/history', { status: f.status, search: f.search }, 50);
        if (rows.length === 0) {
            showToast('Nothing to export', 'warning');
            return;
        }
        const { jsPDF } = window.jspdf;
        const doc = new jsPDF({ orientation: 'landscape' });
        doc.setFillColor(220, 38, 38);
        doc.rect(0, 0, doc.internal.pageSize.getWidth(), 22, 'F');
        doc.setTextColor(255, 255, 255);
        doc.setFontSize(16);
        doc.setFont('helvetica', 'bold');
        doc.text('VitalDrop - Contribution Ledger (Admin)', 14, 14);

        doc.autoTable({
            startY: 28,
            head: [['Date', 'Blood', 'Patient', 'Donator', 'Care Centre', 'Standing']],
            body: rows.map(r => [
                r.status === 'COMPLETED' && r.donationDate ? formatDate(r.donationDate) : formatDate(r.createdAt),
                r.bloodGroup || '-', r.patientName || '-', r.donorName || '-', r.hospitalName || '-',
                (r.status || '').toLowerCase().replace(/_/g, ' '),
            ]),
            theme: 'grid',
            headStyles: { fillColor: [185, 28, 28], textColor: 255, fontStyle: 'bold' },
            styles: { fontSize: 9 },
            margin: { left: 14, right: 14 },
        });
        doc.save('vitaldrop-admin-history-' + new Date().toISOString().slice(0, 10) + '.pdf');
        showToast('PDF exported successfully', 'success');
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
}
