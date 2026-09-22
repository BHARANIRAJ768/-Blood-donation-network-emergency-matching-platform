/* =========================================================================
 * VitalDrop - profile editing, photo upload, password change
 * ========================================================================= */

let pendingPhoto = null;

document.addEventListener('DOMContentLoaded', async () => {
    if (!requireAuth()) return;
    showSpinner(true);
    try {
        await loadUser();
        await setupShell('profile');
        setupProfile();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        showSpinner(false);
    }
});

function setupProfile() {
    document.getElementById('profileEmail').textContent = currentUser.email;
    const roleEl = document.getElementById('roleBadge');
    roleEl.textContent = currentUser.role;
    roleEl.className = 'role-badge role-' + currentUser.role;

    const chip = document.getElementById('profileGroupChip');
    chip.textContent = currentUser.bloodGroup || '—';

    document.getElementById('pName').value = currentUser.name || '';
    document.getElementById('pPhone').value = currentUser.phone || '';
    document.getElementById('pBloodGroup').value = currentUser.bloodGroup || '';
    document.getElementById('pDistrict').value = currentUser.district || '';
    document.getElementById('pAvailable').checked = !!currentUser.available;
    document.getElementById('pAvailableLabel').textContent = currentUser.available ? 'Ready to contribute' : 'Not ready';

    const isDonor = currentUser.role === 'DONOR';
    if (!isDonor) {
        document.getElementById('pAvailable').parentElement.classList.add('d-none');
    }

    if (currentUser.latitude && currentUser.longitude) {
        document.getElementById('pLatitude').value = currentUser.latitude;
        document.getElementById('pLongitude').value = currentUser.longitude;
        document.getElementById('pLocStatus').textContent = 'Location on record';
    }

    renderProfilePhoto(currentUser.photo);

    document.getElementById('pAvailable').addEventListener('change', (e) => {
        document.getElementById('pAvailableLabel').textContent = e.target.checked ? 'Ready to contribute' : 'Not ready';
    });

    /* --- photo --- */
    document.getElementById('photoBtn').addEventListener('click', () => document.getElementById('photoInput').click());
    document.getElementById('photoInput').addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;
        if (file.size > 5 * 1024 * 1024) {
            showToast('Photo must be smaller than 5 MB', 'warning');
            return;
        }
        const reader = new FileReader();
        reader.onload = () => {
            pendingPhoto = reader.result;
            renderProfilePhoto(pendingPhoto);
        };
        reader.readAsDataURL(file);
    });

    /* --- location --- */
    document.getElementById('pLocateBtn').addEventListener('click', () => {
        if (!navigator.geolocation) {
            showToast('Geolocation is not supported by this browser', 'warning');
            return;
        }
        document.getElementById('pLocStatus').textContent = 'Fetching location...';
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                document.getElementById('pLatitude').value = pos.coords.latitude.toFixed(6);
                document.getElementById('pLongitude').value = pos.coords.longitude.toFixed(6);
                document.getElementById('pLocStatus').innerHTML = '<i class="bi bi-check-circle-fill text-success"></i> Location captured';
                showToast('Location updated', 'success');
            },
            () => showToast('Could not get location. Try again later.', 'warning'),
            { enableHighAccuracy: true, timeout: 10000 }
        );
    });

    /* --- save profile --- */
    document.getElementById('profileForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('pName');
        const phone = document.getElementById('pPhone');
        if (!name.value || !name.checkValidity()) { name.classList.add('is-invalid'); showToast('Please enter your full name', 'warning'); return; }
        if (!phone.value || !phone.checkValidity()) { phone.classList.add('is-invalid'); showToast('Please enter a valid phone number', 'warning'); return; }

        const lat = parseFloat(document.getElementById('pLatitude').value);
        const lng = parseFloat(document.getElementById('pLongitude').value);

        setBusy('profileLoader', 'profileBtnText', true, 'Saving...');
        try {
            currentUser = await API.put('/api/user/profile', {
                name: name.value.trim(),
                phone: phone.value.trim(),
                bloodGroup: document.getElementById('pBloodGroup').value || null,
                district: document.getElementById('pDistrict').value.trim() || null,
                latitude: isNaN(lat) ? null : lat,
                longitude: isNaN(lng) ? null : lng,
                available: document.getElementById('pAvailable').checked,
                photo: pendingPhoto,
            });
            showToast('Profile updated successfully', 'success');
            pendingPhoto = null;
            document.getElementById('profileGroupChip').textContent = currentUser.bloodGroup || '—';
            setupShell('profile');
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            setBusy('profileLoader', 'profileBtnText', false, '<i class="bi bi-check2-square me-2"></i>Save Changes');
        }
    });

    /* --- password --- */
    document.getElementById('passwordForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const current = document.getElementById('currentPassword');
        const next = document.getElementById('newPassword');
        const confirm = document.getElementById('confirmNewPassword');

        if (!current.value) { current.classList.add('is-invalid'); return; }
        if (!next.value || next.value.length < 8) { next.classList.add('is-invalid'); return; }
        if (next.value !== confirm.value) {
            confirm.classList.add('is-invalid');
            showToast('Passwords do not match', 'warning');
            return;
        }

        setBusy('pwLoader', 'pwBtnText', true, 'Updating...');
        try {
            await API.put('/api/user/password', { currentPassword: current.value, newPassword: next.value });
            showToast('Password changed successfully', 'success');
            e.target.reset();
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            setBusy('pwLoader', 'pwBtnText', false, '<i class="bi bi-key-fill me-2"></i>Update Password');
        }
    });
}

function renderProfilePhoto(photo) {
    const preview = document.getElementById('photoPreview');
    if (photo) {
        preview.innerHTML = `<img src="${esc(photo)}" alt="Profile" class="profile-photo-lg">`;
    } else {
        preview.innerHTML = '<i class="bi bi-person-vcard"></i>';
    }
}
