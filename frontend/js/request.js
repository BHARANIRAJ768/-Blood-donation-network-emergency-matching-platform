/* =========================================================================
 * VitalDrop - blood assistance request modal + appeal list rendering
 * Shared by home, search and request pages.
 * ========================================================================= */

let requestModal = null;
let requestModalPrefill = {};

function openRequestModal(options) {
    requestModalPrefill = options || {};
    const existing = document.getElementById('bloodRequestModal');
    if (existing) existing.remove();

    const holder = document.createElement('div');
    holder.innerHTML = `
        <div class="modal fade" id="bloodRequestModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-lg req-sheet">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title"><span class="icon-badge plum"><i class="bi bi-send-plus-fill"></i></span>New Blood Assistance Request</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="alert alert-light border small mb-3">
                            <i class="bi bi-info-square-fill me-1"></i>
                            Matching donators within 15 km will be notified immediately.
                        </div>
                        <form id="bloodRequestForm" novalidate>
                            <div class="row g-3 mb-3">
                                <div class="col-lg-6">
                                    <div class="req-section">
                                        <div class="ledger-panel-label"><i class="bi bi-person-vcard-fill me-1"></i>Patient information</div>
                                        <div class="mb-3">
                                            <label class="form-label">Patient Name *</label>
                                            <input type="text" class="form-control" id="reqPatientName" required maxlength="120">
                                            <div class="invalid-feedback">Patient name is required.</div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">Patient Phone *</label>
                                            <input type="tel" class="form-control" id="reqPhone" required>
                                            <div class="invalid-feedback">A contact phone number is required.</div>
                                        </div>
                                        <div class="mb-0">
                                            <label class="form-label">Patient Age</label>
                                            <input type="number" class="form-control" id="reqPatientAge" min="0" max="120" placeholder="e.g. 25">
                                        </div>
                                    </div>
                                </div>
                                <div class="col-lg-6">
                                    <div class="req-section">
                                        <div class="ledger-panel-label"><i class="bi bi-droplet-half me-1"></i>Blood requirement</div>
                                        <div class="mb-3">
                                            <label class="form-label">Blood Group *</label>
                                            <select class="form-select" id="reqBloodGroup" required>
                                                <option value="">Select</option>
                                                <option>A+</option><option>A-</option><option>B+</option><option>B-</option>
                                                <option>AB+</option><option>AB-</option><option>O+</option><option>O-</option>
                                            </select>
                                            <div class="invalid-feedback">Select a blood group.</div>
                                        </div>
                                        <div class="row g-2 mb-3">
                                            <div class="col-6">
                                                <label class="form-label">Units *</label>
                                                <input type="number" class="form-control" id="reqUnits" value="1" min="1" max="10" required>
                                                <div class="invalid-feedback">1 to 10.</div>
                                            </div>
                                            <div class="col-6">
                                                <label class="form-label">Required By</label>
                                                <input type="date" class="form-control" id="reqRequiredDate">
                                            </div>
                                        </div>
                                        <div class="form-check form-switch mb-0">
                                            <input class="form-check-input" type="checkbox" role="switch" id="reqEmergency">
                                            <label class="form-check-label" for="reqEmergency"><span class="emergency-badge">Emergency</span></label>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="req-section">
                                <div class="ledger-panel-label"><i class="bi bi-hospital-fill me-1"></i>Care-centre information</div>
                                <div class="row g-3">
                                    <div class="col-12">
                                        <label class="form-label">Hospital Name *</label>
                                        <input type="text" class="form-control" id="reqHospital" required maxlength="200">
                                        <div class="invalid-feedback">Hospital name is required.</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label">Hospital Address</label>
                                        <input type="text" class="form-control" id="reqHospitalAddress" maxlength="300">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label">Reason</label>
                                        <input type="text" class="form-control" id="reqReason" maxlength="500">
                                    </div>
                                </div>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-quiet" data-bs-dismiss="modal">Cancel</button>
                        <button type="button" class="btn btn-ledger-send" id="submitRequestBtn">
                            <span class="inline-loader" id="reqLoader"></span>
                            <span id="reqBtnText"><i class="bi bi-send-fill me-2"></i>Send Blood Request</span>
                        </button>
                    </div>
                </div>
            </div>
        </div>`;
    document.body.appendChild(holder);
    requestModal = new bootstrap.Modal(document.getElementById('bloodRequestModal'));

    const groupSelect = document.getElementById('reqBloodGroup');
    if (requestModalPrefill.bloodGroup) {
        groupSelect.value = requestModalPrefill.bloodGroup;
    }
    if (currentUser && currentUser.name && currentUser.role === 'PATIENT') {
        document.getElementById('reqPatientName').value = currentUser.name;
        document.getElementById('reqPhone').value = currentUser.phone || '';
    }

    document.getElementById('submitRequestBtn').addEventListener('click', submitBloodRequest);
    document.getElementById('bloodRequestForm').addEventListener('submit', (e) => e.preventDefault());

    requestModal.show();
}

async function submitBloodRequest() {
    const get = id => document.getElementById(id);
    let valid = true;

    const required = [['reqBloodGroup', 'Select a blood group.'], ['reqUnits', 'Enter units between 1 and 10.'],
        ['reqPatientName', 'Patient name is required.'], ['reqPhone', 'A contact phone number is required.'],
        ['reqHospital', 'Hospital name is required.']];
    required.forEach(([id]) => {
        const el = get(id);
        const ok = el.value && el.checkValidity();
        el.classList.toggle('is-invalid', !ok);
        if (!ok) valid = false;
    });

    const units = parseInt(get('reqUnits').value, 10);
    if (isNaN(units) || units < 1 || units > 10) {
        get('reqUnits').classList.add('is-invalid');
        valid = false;
    }

    const phoneVal = get('reqPhone').value.trim();
    if (!/^[0-9]{10}$/.test(phoneVal)) {
        get('reqPhone').classList.add('is-invalid');
        valid = false;
        showToast('Mobile number must be exactly 10 digits', 'warning');
    } else {
        get('reqPhone').classList.remove('is-invalid');
    }

    if (!valid) {
        showToast('Please fix the highlighted fields', 'warning');
        return;
    }

    const lat = currentUser ? currentUser.latitude : null;
    const lng = currentUser ? currentUser.longitude : null;

    const ageVal = get('reqPatientAge').value ? parseInt(get('reqPatientAge').value, 10) : null;
    const reqDateVal = get('reqRequiredDate').value || null;

    setBusy('reqLoader', 'reqBtnText', true, 'Submitting...');
    try {
        const request = await API.post('/api/requests', {
            bloodGroup: get('reqBloodGroup').value,
            hospitalName: get('reqHospital').value.trim(),
            hospitalAddress: get('reqHospitalAddress').value.trim() || null,
            patientName: get('reqPatientName').value.trim(),
            phone: get('reqPhone').value.trim(),
            units,
            reason: get('reqReason').value.trim() || null,
            emergency: get('reqEmergency').checked,
            donorId: requestModalPrefill.donorId || null,
            patientAge: ageVal,
            requiredDate: reqDateVal,
            latitude: lat,
            longitude: lng,
        });
        showToast('Blood assistance request created! Matching donators have been notified.', 'success');
        requestModal.hide();
        showMailSuccessModal();
        if (window.onRequestCreated) window.onRequestCreated(request);
    } catch (err) {
        showToast(err.message, 'error');
        setBusy('reqLoader', 'reqBtnText', false, '<i class="bi bi-send-fill me-2"></i>Send Blood Request');
    }
}

function showMailSuccessModal() {
    const existing = document.getElementById('mailSuccessModal');
    if (existing) existing.remove();
    const holder = document.createElement('div');
    holder.innerHTML = `
        <div class="modal fade" id="mailSuccessModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered" style="max-width:420px">
                <div class="modal-content text-center p-4">
                    <div class="mx-auto mb-3 d-flex align-items-center justify-content-center" style="width:72px;height:72px;border-radius:50%;background:var(--green-bg);color:var(--green);font-size:2.2rem"><i class="bi bi-check2-circle"></i></div>
                    <h5 class="fw-bold">Successfully submitted</h5>
                    <p class="text-muted small mb-1">Mail sent successfully to donator</p>
                    <p class="text-muted small mb-3">The donator has been notified via email.</p>
                    <button class="btn btn-ledger-primary w-100" data-bs-dismiss="modal">Done</button>
                </div>
            </div>
        </div>`;
    document.body.appendChild(holder);
    const modal = new bootstrap.Modal(document.getElementById('mailSuccessModal'));
    modal.show();
    document.getElementById('mailSuccessModal').addEventListener('hidden.bs.modal', () => holder.remove());
}

/* --------------------------- request card rendering --------------------------- */

function requestStatusCard(request) {
    const isPatient = currentUser && request.patientId === currentUser.id;
    const donorInfo = request.donorId ? `
        <div class="d-flex align-items-center gap-2 mt-2">
            <span class="bg-chip">${esc(request.bloodGroup)}</span>
            <div>
                <div class="fw-semibold">${esc(request.donorName)}</div>
                <div class="small text-muted">${esc(request.donorPhone || '')}</div>
            </div>
            ${isPatient && request.status === 'ACCEPTED' ? `
                <a href="tel:${esc(request.donorPhone)}" class="btn btn-success-soft btn-sm ms-auto">
                    <i class="bi bi-telephone-fill me-1"></i>Call
                </a>` : ''}
        </div>` : '';

    let actions = '';
    if (isPatient) {
        if (request.status === 'PENDING') {
            actions = `<button class="btn btn-quiet btn-sm" data-act="cancel" data-id="${request.id}">Cancel</button>`;
        }
        if (request.status === 'ACCEPTED') {
            actions = `<button class="btn btn-ledger-primary btn-sm" data-act="complete" data-id="${request.id}">
                            <i class="bi bi-check2-square me-1"></i>Mark Contribution Completed</button>`;
        }
    } else if (request.status === 'PENDING') {
        actions = `
            <button class="btn btn-go btn-sm" data-act="accept" data-id="${request.id}"><i class="bi bi-check-lg me-1"></i>Accept</button>
            <button class="btn btn-stop btn-sm" data-act="reject" data-id="${request.id}"><i class="bi bi-x-lg me-1"></i>Decline</button>`;
    }

    return `
        <div class="card-lifelink p-3 mb-3 animate-in">
            <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
                <div>
                    <div class="d-flex align-items-center gap-2">
                        <span class="bg-chip">${esc(request.bloodGroup)}</span>
                        <span class="fw-bold">${esc(request.hospitalName)}</span>
                        ${request.emergency ? '<span class="emergency-badge">Emergency</span>' : ''}
                    </div>
                    <div class="text-muted small mt-1">${esc(request.hospitalAddress || '')}</div>
                    <div class="small mt-1"><strong>Patient:</strong> ${esc(request.patientName)}
                        &middot; <strong>Units:</strong> ${request.units}
                        &middot; <strong>Created:</strong> ${formatDateTime(request.createdAt)}</div>
                    ${request.reason ? `<div class="small text-muted mt-1"><i class="bi bi-chat-square-text-fill me-1"></i>${esc(request.reason)}</div>` : ''}
                </div>
                <div class="text-end">
                    ${statusBadge(request.status)}
                    ${donorInfo}
                </div>
            </div>
            <div class="d-flex gap-2 mt-3">
                ${actions}
            </div>
        </div>`;
}

function renderRequestList(requests, containerId, emptyHtml) {
    const container = document.getElementById(containerId);
    if (!container) return;
    if (!requests || requests.length === 0) {
        container.innerHTML = emptyHtml || `
            <div class="empty-state">
                <div class="empty-icon">&#128203;</div>
                <div>No requests yet.</div>
            </div>`;
        return;
    }
    container.innerHTML = requests.map(requestStatusCard).join('');
}

async function runRequestAction(requestId, action) {
    showSpinner(true);
    try {
        if (action === 'accept') await API.put('/api/requests/' + requestId + '/accept');
        if (action === 'reject') await API.put('/api/requests/' + requestId + '/reject');
        if (action === 'complete') await API.put('/api/requests/' + requestId + '/complete');
        if (action === 'cancel') await API.put('/api/requests/' + requestId + '/cancel');
        const messages = { accept: 'Appeal accepted', reject: 'Appeal declined', complete: 'Blood contribution marked complete', cancel: 'Appeal cancelled' };
        showToast(messages[action], action === 'reject' ? 'warning' : 'success');
        if (window.onRequestListChanged) window.onRequestListChanged();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
}

/* Delegate clicks on any container that holds request action buttons */
document.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-act]');
    if (!btn) return;
    runRequestAction(btn.dataset.id, btn.dataset.act);
});
