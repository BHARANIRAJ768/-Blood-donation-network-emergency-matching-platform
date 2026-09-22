/* =========================================================================
 * VitalDrop - advanced donator search
 * ========================================================================= */

let searchLocation = null;

document.addEventListener('DOMContentLoaded', async () => {
    if (!requireAuth()) return;
    showSpinner(true);
    try {
        await loadUser();
        await setupShell('search');
        setupSearch();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
});

function setupSearch() {
    const params = new URLSearchParams(window.location.search);
    const preset = params.get('bloodGroup');
    if (preset) document.getElementById('searchBloodGroup').value = preset;

    document.getElementById('searchBtn').addEventListener('click', () => runSearch(0));
    document.getElementById('searchBloodGroup').addEventListener('change', () => runSearch(0));
    document.getElementById('searchClearLoc').addEventListener('click', () => {
        searchLocation = null;
        document.getElementById('searchLocText').value = currentUser.latitude ? 'Using saved location' : 'Location not set';
        runSearch(0);
    });
    document.getElementById('searchLocateBtn').addEventListener('click', () => {
        if (!navigator.geolocation) {
            showToast('Geolocation is not supported by this browser', 'warning');
            return;
        }
        document.getElementById('searchLocText').value = 'Fetching location...';
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                searchLocation = { latitude: pos.coords.latitude, longitude: pos.coords.longitude };
                document.getElementById('searchLocText').value = 'Location captured';
                showToast('Using your current location', 'success');
                runSearch(0);
            },
            () => showToast('Could not get location. Using your saved location.', 'warning'),
            { enableHighAccuracy: true, timeout: 10000 }
        );
    });

    if (currentUser.latitude && currentUser.longitude) {
        document.getElementById('searchLocText').value = 'Using your saved location';
    } else {
        document.getElementById('searchLocText').value = 'Location not set';
    }

    runSearch(0);
}

async function runSearch(page) {
    showSpinner(true);
    try {
        const bloodGroup = document.getElementById('searchBloodGroup').value || '';
        const params = new URLSearchParams({ page: String(page), size: '12' });
        if (bloodGroup) params.set('bloodGroup', bloodGroup);
        if (searchLocation) {
            params.set('latitude', String(searchLocation.latitude));
            params.set('longitude', String(searchLocation.longitude));
        }
        const data = await API.get('/api/search?' + params.toString());
        renderDonorGrid(data.content, 'searchGrid');
        renderPagination(data, 'searchPagination', (p) => runSearch(p));
        document.getElementById('searchResultInfo').textContent =
            `${data.totalElements} available donator${data.totalElements === 1 ? '' : 's'} found`;
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
}
