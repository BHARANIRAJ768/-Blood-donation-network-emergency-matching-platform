/* LifeLink end-to-end smoke test — every API and flow is exercised. */
const BASE = 'http://localhost:8080';

let passed = 0;
let failed = 0;
let step = 0;

function check(name, condition, extra) {
    step++;
    if (condition) {
        passed++;
        console.log(`  PASS  [${step}] ${name}${extra ? ' — ' + extra : ''}`);
    } else {
        failed++;
        console.log(`  FAIL  [${step}] ${name}${extra ? ' — ' + extra : ''}`);
    }
}

async function api(method, path, body, token) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers.Authorization = 'Bearer ' + token;
    const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
    const ct = res.headers.get('content-type') || '';
    const data = ct.includes('application/json') ? await res.json() : null;
    return { status: res.status, data };
}

const stamp = Date.now();
const email = (name) => `${name}${stamp}@test.com`;
const uniq = (name) => `smoke-${name}-${stamp}`;

const DONOR1 = { name: uniq('donor1'), email: email('donor1'), phone: '9100' + stamp.toString().slice(-6), password: 'Password@123', role: 'DONOR', bloodGroup: 'A+', district: 'Chennai', latitude: 13.0827, longitude: 80.2707, available: true };
const DONOR2 = { name: uniq('donor2'), email: email('donor2'), phone: '9101' + stamp.toString().slice(-6), password: 'Password@123', role: 'DONOR', bloodGroup: 'A+', district: 'Bangalore', latitude: 12.9716, longitude: 77.5946, available: true };
const DONOR3 = { name: uniq('donor3'), email: email('donor3'), phone: '9102' + stamp.toString().slice(-6), password: 'Password@123', role: 'DONOR', bloodGroup: 'B+', district: 'Chennai', latitude: 13.0827, longitude: 80.2707, available: true };
const PATIENT = { name: uniq('patient'), email: email('patient'), phone: '9103' + stamp.toString().slice(-6), password: 'Password@123', role: 'PATIENT', bloodGroup: 'A+', district: 'Chennai', latitude: 13.0827, longitude: 80.2707, available: false };

const token = { donor1: null, donor2: null, donor3: null, patient: null, admin: null };

async function main() {
    console.log(`\nLifeLink smoke test (${new Date().toISOString()})\n`);

    /* ---------- register ---------- */
    let r = await api('POST', '/api/auth/register', DONOR1);
    check('register donor1', r.status === 201 && !!r.data.token, r.status);
    token.donor1 = r.data?.token;

    r = await api('POST', '/api/auth/register', DONOR2);
    check('register donor2', r.status === 201 && !!r.data.token, r.status);
    token.donor2 = r.data?.token;

    r = await api('POST', '/api/auth/register', DONOR3);
    check('register donor3', r.status === 201 && !!r.data.token, r.status);
    token.donor3 = r.data?.token;

    r = await api('POST', '/api/auth/register', PATIENT);
    check('register patient', r.status === 201 && !!r.data.token, r.status);
    token.patient = r.data?.token;

    /* duplicate email must be rejected */
    r = await api('POST', '/api/auth/register', DONOR1);
    check('duplicate email rejected (409)', r.status === 409, r.status);

    /* duplicate phone must be rejected */
    r = await api('POST', '/api/auth/register', { ...DONOR2, email: email('dup') });
    check('duplicate phone rejected (409)', r.status === 409, r.status);

    /* invalid password length rejected */
    r = await api('POST', '/api/auth/register', { ...DONOR1, email: email('short'), password: 'short' });
    check('short password rejected (400)', r.status === 400, r.status);

    /* donor without blood group rejected */
    r = await api('POST', '/api/auth/register', { ...DONOR1, email: email('nogroup'), phone: '9300' + stamp.toString().slice(-6), bloodGroup: null });
    check('donor without blood group rejected (400)', r.status === 400, r.status);

    /* ---------- login ---------- */
    r = await api('POST', '/api/auth/login', { email: PATIENT.email, password: 'Password@123', remember: true });
    check('login patient', r.status === 200 && !!r.data.token, r.status);
    token.patient = r.data?.token;

    r = await api('POST', '/api/auth/login', { email: PATIENT.email, password: 'wrongpass' });
    check('login wrong password rejected', r.status === 400 || r.status === 401, r.status);

    /* ---------- auth guard ---------- */
    r = await api('GET', '/api/user/profile');
    check('profile requires auth (401)', r.status === 401, r.status);

    r = await api('GET', '/api/user/profile', null, token.patient);
    check('authenticated profile', r.status === 200 && r.data.email === PATIENT.email, r.status);

    /* ---------- search ---------- */
    r = await api('GET', `/api/search?bloodGroup=A%2B&page=0&size=10`, null, token.patient);
    check('search A+ donors', r.status === 200 && r.data.totalElements >= 2, r.status);
    const searchList = r.data.content || [];
    const d1 = searchList.find(d => d.name === DONOR1.name);
    const d2 = searchList.find(d => d.name === DONOR2.name);
    const idx1 = searchList.indexOf(d1);
    const idx2 = searchList.indexOf(d2);
    check('near donor ranked before far donor', d1 && d2 && idx1 !== -1 && idx2 !== -1 && idx1 < idx2, `${DONOR1.district}=${idx1}, ${DONOR2.district}=${idx2}`);
    check('distance computed (near donor < far donor)',
        d1 && d2 && d1.distanceKm < d2.distanceKm, `near=${d1?.distanceKm} far=${d2?.distanceKm}`);
    check('donor3 (B+) excluded', !searchList.some(d => d.name === DONOR3.name));

    r = await api('GET', `/api/donors?bloodGroup=B%2B`, null, token.patient);
    check('/api/donors B+ returns donor3', r.status === 200 && r.data.content.some(d => d.name === DONOR3.name), r.status);

    /* ---------- request creation ---------- */
    const reqBody = {
        bloodGroup: 'A+',
        hospitalName: 'Apollo Hospital Chennai',
        hospitalAddress: '21 Greams Lane, Chennai',
        patientName: PATIENT.name,
        phone: PATIENT.phone,
        units: 2,
        reason: 'Surgery scheduled',
        emergency: true,
        latitude: PATIENT.latitude,
        longitude: PATIENT.longitude,
    };
    r = await api('POST', '/api/requests', reqBody, token.patient);
    check('create blood request (201)', r.status === 201 && r.data.status === 'PENDING', r.status);
    const requestId = r.data?.id;
    check('request id present', !!requestId, requestId);

    /* ---------- donor notifications ---------- */
    r = await api('GET', '/api/notifications?limit=10', null, token.donor1);
    check('donor1 (near, A+) notified', r.status === 200 && r.data.some(n => n.requestId === requestId), r.status);
    const notif = (r.data || []).find(n => n.requestId === requestId);
    check('donor1 notification title is Emergency', notif && notif.title === 'Emergency Blood Request', notif?.title);
    check('donor1 unread count = 1', (await api('GET', '/api/notifications/unread-count', null, token.donor1)).data?.count >= 1);

    r = await api('GET', '/api/notifications?limit=10', null, token.donor2);
    check('donor2 (far, >15km) NOT notified', r.status === 200 && !r.data.some(n => n.requestId === requestId), r.status);

    r = await api('GET', '/api/notifications?limit=10', null, token.donor3);
    check('donor3 (wrong group) NOT notified', r.status === 200 && !r.data.some(n => n.requestId === requestId), r.status);

    /* donor board: donor3 should NOT see the A+ request */
    r = await api('GET', '/api/requests/available', null, token.donor3);
    check('donor3 available list excludes A+ request', r.status === 200 && !r.data.some(x => x.id === requestId), r.status);

    /* ---------- donor rejects via notification ---------- */
    r = await api('GET', '/api/notifications?limit=10', null, token.donor1);
    const pendingNotif = (r.data || []).find(n => n.requestId === requestId && n.title === 'Emergency Blood Request');
    if (pendingNotif) {
        const rejectResp = await api('POST', `/api/notifications/${pendingNotif.id}/reject`, null, token.donor1);
        check('donor rejects via bell', rejectResp.status === 200, rejectResp.status);
        check('request stays PENDING after decline', (await api('GET', '/api/requests/mine', null, token.patient)).data.find(x => x.id === requestId)?.status === 'PENDING');
        check('declined donor no longer on board', !(await api('GET', '/api/requests/available', null, token.donor1)).data.some(x => x.id === requestId));
    } else {
        check('donor rejects via bell', false, 'notification not found');
    }

    /* ---------- donor accepts (fresh request to test accept path) ---------- */
    const reqBody2 = { ...reqBody, units: 1, reason: 'Surgery scheduled', emergency: false };
    r = await api('POST', '/api/requests', reqBody2, token.patient);
    const requestId2 = r.data?.id;
    check('second request created', r.status === 201 && !!requestId2, r.status);

    r = await api('GET', '/api/notifications?limit=10', null, token.donor1);
    const acceptNotif = (r.data || []).find(n => n.requestId === requestId2);
    check('donor1 notified for second request', !!acceptNotif, acceptNotif?.id);

    r = await api('POST', `/api/notifications/${acceptNotif.id}/accept`, null, token.donor1);
    check('donor accepts via bell', r.status === 200 && r.data.status === 'ACCEPTED', r.status);
    check('request assigned to donor1', r.data?.donorId != null, r.data?.donorName);

    /* patient sees accepted request */
    r = await api('GET', '/api/requests/mine', null, token.patient);
    const accepted = (r.data || []).find(x => x.id === requestId2);
    check('patient sees ACCEPTED with donor details', accepted && accepted.status === 'ACCEPTED'
        && accepted.donorName === DONOR1.name && accepted.donorPhone === DONOR1.phone, accepted?.status);

    /* patient notification about acceptance */
    r = await api('GET', '/api/notifications?limit=10', null, token.patient);
    check('patient notified about acceptance', r.status === 200 && r.data.some(n => n.requestId === requestId2 && n.title.includes('accepted')), r.status);

    /* ---------- complete ---------- */
    r = await api('PUT', `/api/requests/${requestId2}/complete`, null, token.patient);
    check('complete request', r.status === 200 && r.data.status === 'COMPLETED', r.status);

    /* ---------- history ---------- */
    r = await api('GET', '/api/history?status=COMPLETED&page=0&size=10', null, token.patient);
    check('patient history has COMPLETED row', r.status === 200 && r.data.content.some(h => h.id === requestId2 && h.myRole === 'PATIENT'), r.status);

    r = await api('GET', '/api/history?status=COMPLETED&page=0&size=10', null, token.donor1);
    check('donor history has COMPLETED row', r.status === 200 && r.data.content.some(h => h.id === requestId2 && h.myRole === 'DONOR'), r.status);

    r = await api('GET', '/api/history?search=Apollo&page=0&size=10', null, token.patient);
    check('history search by hospital', r.status === 200 && r.data.totalElements >= 1, r.status);

    /* donor last donation date updated */
    r = await api('GET', '/api/user/profile', null, token.donor1);
    check('donor lastDonationDate set to today', r.data?.lastDonationDate === new Date().toISOString().slice(0, 10), r.data?.lastDonationDate);

    /* ---------- profile update ---------- */
    r = await api('PUT', '/api/user/profile', { name: DONOR1.name, phone: DONOR1.phone, bloodGroup: 'A+', district: 'Chennai', latitude: 13.0827, longitude: 80.2707, available: true }, token.donor1);
    check('update profile', r.status === 200 && r.data.name === DONOR1.name, r.status);

    r = await api('PUT', '/api/user/availability', { available: false }, token.donor1);
    check('toggle availability off', r.status === 200 && r.data.available === false, r.status);
    r = await api('PUT', '/api/user/availability', { available: true }, token.donor1);
    check('toggle availability on', r.status === 200 && r.data.available === true, r.status);

    /* ---------- forgot password ---------- */
    r = await api('POST', '/api/auth/forgot-password', { email: PATIENT.email });
    check('forgot password generic response', r.status === 200 && !!r.data.message, r.status);

    /* ---------- admin ---------- */
    r = await api('POST', '/api/auth/login', { email: 'admin@lifelink.com', password: 'Admin@123' });
    check('admin login', r.status === 200 && !!r.data.token, r.status);
    token.admin = r.data?.token;

    r = await api('GET', '/api/admin/stats', null, token.admin);
    check('admin stats', r.status === 200 && r.data.totalUsers >= 4 && r.data.completedRequests >= 1, JSON.stringify(r.data));

    r = await api('GET', '/api/admin/users?search=smoke&page=0&size=10', null, token.admin);
    check('admin users list + search', r.status === 200 && r.data.content.length >= 4, r.data.totalElements);

    r = await api('GET', '/api/admin/requests?status=COMPLETED&page=0&size=10', null, token.admin);
    check('admin requests filtered', r.status === 200 && r.data.content.some(x => x.id === requestId2), r.status);

    r = await api('GET', '/api/admin/history?page=0&size=10', null, token.admin);
    check('admin history', r.status === 200 && r.data.content.length >= 1, r.status);

    /* non-admin forbidden from admin API */
    r = await api('GET', '/api/admin/stats', null, token.patient);
    check('non-admin blocked from admin API (403)', r.status === 403, r.status);

    /* ---------- static pages ---------- */
    for (const page of ['/', '/login.html', '/register.html', '/home.html', '/search.html', '/profile.html', '/request.html', '/history.html', '/admin.html', '/reset-password.html']) {
        const res = await fetch(BASE + page);
        check(`page ${page} served`, res.status === 200, res.status);
    }

    /* ---------- summary ---------- */
    console.log(`\n========================================`);
    console.log(`  PASSED: ${passed}   FAILED: ${failed}`);
    console.log(`========================================\n`);
    process.exit(failed === 0 ? 0 : 1);
}

main().catch((err) => {
    console.error('SMOKE TEST CRASHED:', err);
    process.exit(1);
});
