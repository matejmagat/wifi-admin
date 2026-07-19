import { apiFetch } from './client';

export function getWifiConfiguration(cpeId) {
    return apiFetch(`/wifi-parameter/${encodeURIComponent(cpeId)}`, {
        method: 'GET',
    });
}

export function updateWifiConfiguration(config) {
    return apiFetch('/wifi-parameter', {
        method: 'PUT',
        body: JSON.stringify(config),
    });
}

export function getEncryptionTypes() {
    return apiFetch('/wifi-parameter/encryption-types', {
        method: 'GET',
    });
}

export function getWifiBandTypes() {
    return apiFetch('/wifi-parameter/wifi-band-types', {
        method: 'GET',
    });
}