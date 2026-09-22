/* =========================================================================
 * VitalDrop - home dashboard
 * ========================================================================= */

let homeLocation = null;

document.addEventListener('DOMContentLoaded', async () => {
    if (!requireAuth()) return;
    showSpinner(true);
    try {
        await loadUser();
        await setupShell('home');
        await setupHome();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
});

async function setupHome() {
    const hour = new Date().getHours();
    const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
    document.getElementById('greeting').textContent = `${greeting}, ${currentUser.name.split(' ')[0]}`;
    document.getElementById('userLine').textContent =
        `${currentUser.role === 'DONOR' ? 'Donator' : 'Patient'} &middot; ${esc(currentUser.email)}`;

    /* availability toggle (donors) */
    if (currentUser.role === 'DONOR') {
        const wrap = document.getElementById('availToggleWrap');
        wrap.classList.remove('d-none');
        const toggle = document.getElementById('availToggle');
        toggle.checked = currentUser.available;
        document.getElementById('availLabel').textContent = currentUser.available ? 'Available' : 'Unavailable';
        toggle.addEventListener('change', async () => {
            try {
                currentUser = await API.put('/api/user/availability', { available: toggle.checked });
                document.getElementById('availLabel').textContent = toggle.checked ? 'Available' : 'Unavailable';
                showToast(toggle.checked ? 'You are now marked available' : 'You are now marked unavailable',
                    toggle.checked ? 'success' : 'warning');
            } catch (err) {
                toggle.checked = !toggle.checked;
                showToast(err.message, 'error');
            }
        });
    }

    /* last donation reminder (>90 days) */
    if (currentUser.role === 'DONOR' && currentUser.lastDonationDate) {
        const days = Math.floor((Date.now() - new Date(currentUser.lastDonationDate).getTime()) / 86400000);
        if (days > 90) document.getElementById('reminderBanner').classList.remove('d-none');
    }

    document.getElementById('newRequestBtn').addEventListener('click', () => openRequestModal({}));

    document.getElementById('homeLocateBtn').addEventListener('click', captureLocation);
    document.getElementById('homeSearchBtn').addEventListener('click', () => loadDonors(0));
    document.getElementById('homeBloodGroup').addEventListener('change', () => loadDonors(0));
    document.getElementById('homeSearchForm').addEventListener('submit', (e) => {
        e.preventDefault();
        loadDonors(0);
    });

    if (currentUser.latitude && currentUser.longitude) {
        document.getElementById('homeLocText').value = 'Using your saved location';
    } else {
        document.getElementById('homeLocText').value = 'Location not set';
    }

    await loadDonors(0);
}

async function captureLocation() {
    if (!navigator.geolocation) {
        showToast('Geolocation is not supported by this browser', 'warning');
        return;
    }
    document.getElementById('homeLocText').value = 'Fetching location...';
    navigator.geolocation.getCurrentPosition(
        async (pos) => {
            homeLocation = { latitude: pos.coords.latitude, longitude: pos.coords.longitude };
            document.getElementById('homeLocText').value = 'Location captured';
            showToast('Using your current location', 'success');
            await loadDonors(0);
        },
        () => {
            document.getElementById('homeLocText').value = currentUser.latitude ? 'Using saved location' : 'Location not set';
            showToast('Could not get location. Using your saved location.', 'warning');
        },
        { enableHighAccuracy: true, timeout: 10000 }
    );
}

async function loadDonors(page) {
    showSpinner(true);
    try {
        const bloodGroup = document.getElementById('homeBloodGroup').value || '';
        const params = new URLSearchParams({ page: String(page), size: '12' });
        if (bloodGroup) params.set('bloodGroup', bloodGroup);
        if (homeLocation) {
            params.set('latitude', String(homeLocation.latitude));
            params.set('longitude', String(homeLocation.longitude));
        }
        const data = await API.get('/api/search?' + params.toString());
        renderDonorGrid(data.content, 'donorGrid');
        renderPagination(data, 'homePagination', (p) => loadDonors(p));
        document.getElementById('homeResultInfo').textContent =
            `${data.totalElements} available donator${data.totalElements === 1 ? '' : 's'} found`;
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
}
