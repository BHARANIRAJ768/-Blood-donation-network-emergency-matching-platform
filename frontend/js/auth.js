/* =========================================================================
 * VitalDrop - authentication flows: login, register, forgot & reset password
 * ========================================================================= */

document.addEventListener('DOMContentLoaded', () => {
    initTheme();

    const params = new URLSearchParams(window.location.search);
    if (params.get('expired') === '1') {
        showToast('Your session has expired. Please log in again.', 'warning');
    }

    if (document.getElementById('loginForm')) setupLogin();
    if (document.getElementById('registerForm')) setupRegister();
    if (document.getElementById('resetForm')) setupReset();
});

/* --------------------------- login --------------------------- */

function setupLogin() {
    if (API.isAuthenticated()) {
        window.location.href = 'home.html';
        return;
    }

    document.getElementById('togglePassword').addEventListener('click', () => {
        const input = document.getElementById('password');
        const icon = document.querySelector('#togglePassword i');
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
    });

    document.getElementById('loginForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email');
        const password = document.getElementById('password');

        let valid = true;
        if (!email.value || !email.checkValidity()) {
            email.classList.add('is-invalid');
            valid = false;
        }
        if (!password.value) {
            password.classList.add('is-invalid');
            valid = false;
        }
        if (!valid) return;

        setBusy('loginLoader', 'loginText', true, 'Logging in...');
        try {
            const data = await API.post('/api/auth/login', {
                email: email.value.trim(),
                password: password.value,
                remember: document.getElementById('remember').checked,
            });
            API.setToken(data.token, document.getElementById('remember').checked);
            showToast('Welcome back!', 'success');
            setTimeout(() => (window.location.href = 'home.html'), 400);
        } catch (err) {
            showToast(err.message, 'error');
            setBusy('loginLoader', 'loginText', false, '<i class="bi bi-box-arrow-in-right me-2"></i>Log in');
        }
    });

    document.getElementById('forgotForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('forgotEmail');
        if (!email.value || !email.checkValidity()) {
            email.classList.add('is-invalid');
            return;
        }
        setBusy('forgotLoader', 'forgotText', true, 'Sending...');
        try {
            await API.post('/api/auth/forgot-password', { email: email.value.trim() });
            showToast('If that email exists, a reset link has been sent.', 'success');
            const modal = bootstrap.Modal.getInstance(document.getElementById('forgotModal'));
            modal.hide();
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            setBusy('forgotLoader', 'forgotText', false, 'Send reset link');
        }
    });
}

/* --------------------------- register --------------------------- */

function setupRegister() {
    if (API.isAuthenticated()) {
        window.location.href = 'home.html';
        return;
    }

    let role = 'DONOR';
    const roleDonor = document.getElementById('roleDonor');
    const rolePatient = document.getElementById('rolePatient');
    const availabilityBox = document.getElementById('availabilityBox');

    function selectRole(next) {
        role = next;
        roleDonor.classList.toggle('selected', role === 'DONOR');
        rolePatient.classList.toggle('selected', role === 'PATIENT');
        document.getElementById('role').value = role;
        availabilityBox.style.display = role === 'DONOR' ? '' : 'none';
    }

    roleDonor.addEventListener('click', () => selectRole('DONOR'));
    rolePatient.addEventListener('click', () => selectRole('PATIENT'));

    document.getElementById('available').addEventListener('change', (e) => {
        document.getElementById('availableLabel').textContent = e.target.checked ? 'Available to donate' : 'Unavailable';
    });

    /* --- location --- */
    const locStatus = document.getElementById('locStatus');
    const latField = document.getElementById('latitude');
    const lngField = document.getElementById('longitude');

    document.getElementById('locateBtn').addEventListener('click', () => {
        if (!navigator.geolocation) {
            locStatus.textContent = 'Geolocation not supported - enter manually';
            document.getElementById('manualLocBox').classList.remove('d-none');
            return;
        }
        locStatus.textContent = 'Fetching location...';
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                latField.value = pos.coords.latitude.toFixed(6);
                lngField.value = pos.coords.longitude.toFixed(6);
                locStatus.innerHTML = '<i class="bi bi-check-circle-fill text-success"></i> Location captured';
            },
            (err) => {
                locStatus.textContent = 'Could not get location - please enter manually';
                document.getElementById('manualLocBox').classList.remove('d-none');
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
        );
    });

    document.getElementById('manualLocBtn').addEventListener('click', () => {
        document.getElementById('manualLocBox').classList.remove('d-none');
    });

    document.getElementById('setManualLoc').addEventListener('click', () => {
        const lat = parseFloat(document.getElementById('manualLat').value);
        const lng = parseFloat(document.getElementById('manualLng').value);
        if (isNaN(lat) || isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            showToast('Please enter valid coordinates (lat -90..90, lng -180..180)', 'warning');
            return;
        }
        latField.value = lat.toFixed(6);
        lngField.value = lng.toFixed(6);
        locStatus.innerHTML = '<i class="bi bi-check-circle-fill text-success"></i> Manual location set';
        document.getElementById('manualLocBox').classList.add('d-none');
    });

    /* --- submit --- */
    document.getElementById('registerForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const fields = ['name', 'regEmail', 'phone', 'regPassword', 'confirmPassword'];
        const get = id => document.getElementById(id);
        let valid = true;

        [get('name'), get('regEmail'), get('phone'), get('regPassword')].forEach(input => {
            const ok = input.value && input.checkValidity();
            input.classList.toggle('is-invalid', !ok);
            if (!ok) valid = false;
        });

        const password = get('regPassword');
        const confirm = get('confirmPassword');
        if (password.value !== confirm.value) {
            confirm.classList.add('is-invalid');
            valid = false;
        } else {
            confirm.classList.remove('is-invalid');
        }

        const bloodGroup = get('bloodGroup').value;
        if (role === 'DONOR' && !bloodGroup) {
            get('bloodGroup').classList.add('is-invalid');
            valid = false;
        } else {
            get('bloodGroup').classList.remove('is-invalid');
        }

        if (!valid) {
            showToast('Please fix the highlighted fields', 'warning');
            return;
        }

        setBusy('registerLoader', 'registerText', true, 'Creating account...');
        try {
            const data = await API.post('/api/auth/register', {
                name: get('name').value.trim(),
                email: get('regEmail').value.trim(),
                phone: get('phone').value.trim(),
                password: password.value,
                role,
                bloodGroup: bloodGroup || null,
                district: get('district').value.trim() || null,
                latitude: latField.value ? parseFloat(latField.value) : null,
                longitude: lngField.value ? parseFloat(lngField.value) : null,
                available: role === 'DONOR' ? get('available').checked : false,
            });
            API.setToken(data.token, false);
            showToast('Account created! Welcome to VitalDrop.', 'success');
            setTimeout(() => (window.location.href = 'home.html'), 500);
        } catch (err) {
            showToast(err.message, 'error');
            setBusy('registerLoader', 'registerText', false, '<i class="bi bi-person-plus-fill me-2"></i>Register');
        }
    });
}

/* --------------------------- reset password --------------------------- */

function setupReset() {
    const token = new URLSearchParams(window.location.search).get('token');
    if (!token) {
        showToast('This reset link is invalid or incomplete.', 'error');
        document.getElementById('resetForm').querySelector('button[type=submit]').disabled = true;
        return;
    }

    document.getElementById('resetForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const np = document.getElementById('newPassword');
        const cp = document.getElementById('confirmNewPassword');

        let valid = np.value && np.checkValidity();
        np.classList.toggle('is-invalid', !valid);
        if (np.value !== cp.value) {
            cp.classList.add('is-invalid');
            valid = false;
        } else {
            cp.classList.remove('is-invalid');
        }
        if (!valid) return;

        setBusy('resetLoader', 'resetText', true, 'Updating...');
        try {
            await API.post('/api/auth/reset-password', { token, newPassword: np.value });
            showToast('Password updated. You can now log in.', 'success');
            setTimeout(() => (window.location.href = 'login.html'), 800);
        } catch (err) {
            showToast(err.message, 'error');
            setBusy('resetLoader', 'resetText', false, '<i class="bi bi-check-circle me-2"></i>Update password');
        }
    });
}

/* --------------------------- helpers --------------------------- */

function setBusy(loaderId, textId, busy, text) {
    document.getElementById(loaderId).classList.toggle('show', busy);
    const textEl = document.getElementById(textId);
    if (text) textEl.innerHTML = text;
    textEl.style.opacity = busy ? 0.6 : 1;
}
