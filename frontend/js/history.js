/* =========================================================================
 * VitalDrop - blood contribution history table, filters, pagination and PDF export
 * ========================================================================= */

let historyFilters = { status: '', search: '' };

document.addEventListener('DOMContentLoaded', async () => {
    if (!requireAuth()) return;
    showSpinner(true);
    try {
        await loadUser();
        await setupShell('history');
        setupHistory();
        await loadHistory(0);
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
});

function setupHistory() {
    document.getElementById('historySearchBtn').addEventListener('click', () => {
        historyFilters = {
            status: document.getElementById('historyStatus').value,
            search: document.getElementById('historySearch').value.trim(),
        };
        loadHistory(0);
    });
    document.getElementById('historyStatus').addEventListener('change', () => {
        historyFilters.status = document.getElementById('historyStatus').value;
        loadHistory(0);
    });
    document.getElementById('historySearch').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') document.getElementById('historySearchBtn').click();
    });
    document.getElementById('exportPdfBtn').addEventListener('click', exportPdf);
}

async function loadHistory(page) {
    showSpinner(true);
    try {
        const params = new URLSearchParams({ page: String(page), size: '10' });
        if (historyFilters.status) params.set('status', historyFilters.status);
        if (historyFilters.search) params.set('search', historyFilters.search);
        const data = await API.get('/api/history?' + params.toString());
        renderHistoryRows(data.content);
        renderPagination(data, 'historyPagination', (p) => loadHistory(p));
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
}

function renderHistoryRows(rows) {
    const body = document.getElementById('historyBody');
    if (!rows || rows.length === 0) {
        body.innerHTML = `<tr><td colspan="7">
            <div class="empty-state"><div class="empty-icon">&#128209;</div><div class="fw-semibold">Ledger is empty</div><div class="small text-muted">No records match your filters.</div></div>
        </td></tr>`;
        return;
    }
    body.innerHTML = rows.map(row => {
        const date = row.status === 'COMPLETED' && row.donationDate
            ? formatDate(row.donationDate) : formatDate(row.createdAt);
        const myRole = row.myRole ? roleBadge(row.myRole) : '<span class="text-muted">-</span>';
        return `
            <tr>
                <td class="text-nowrap">${esc(date)}</td>
                <td><span class="bg-chip">${esc(row.bloodGroup || '—')}</span></td>
                <td>${esc(row.patientName || '—')}${row.emergency ? ' <span class="emergency-badge">Emergency</span>' : ''}</td>
                <td>${esc(row.donorName || '—')}</td>
                <td>${esc(row.hospitalName || '—')}</td>
                <td>${statusBadge(row.status)}</td>
                <td>${myRole}</td>
            </tr>`;
    }).join('');
}

async function exportPdf() {
    showSpinner(true);
    try {
        const allRows = [];
        let page = 0;
        const size = 50;
        for (;;) {
            const params = new URLSearchParams({ page: String(page), size: String(size) });
            if (historyFilters.status) params.set('status', historyFilters.status);
            if (historyFilters.search) params.set('search', historyFilters.search);
            const data = await API.get('/api/history?' + params.toString());
            allRows.push(...data.content);
            if (page + 1 >= data.totalPages) break;
            page += 1;
        }

        if (allRows.length === 0) {
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
        doc.text('VitalDrop - Blood Contribution History', 14, 14);

        const head = [['Date', 'Blood Group', 'Patient', 'Donator', 'Care Centre', 'Standing']];
        const body = allRows.map(row => [
            row.status === 'COMPLETED' && row.donationDate ? formatDate(row.donationDate) : formatDate(row.createdAt),
            row.bloodGroup || '-',
            row.patientName || '-',
            row.donorName || '-',
            row.hospitalName || '-',
            (row.status || '').toLowerCase().replace(/_/g, ' '),
        ]);

        doc.autoTable({
            startY: 28,
            head,
            body,
            theme: 'grid',
            headStyles: { fillColor: [185, 28, 28], textColor: 255, fontStyle: 'bold' },
            styles: { fontSize: 9 },
            margin: { left: 14, right: 14 },
        });
        doc.save('vitaldrop-history-' + new Date().toISOString().slice(0, 10) + '.pdf');
        showToast('PDF exported successfully', 'success');
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
}
